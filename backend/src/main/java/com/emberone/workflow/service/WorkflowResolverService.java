/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.service;

import com.emberone.workflow.domain.*;
import com.emberone.workflow.exception.WorkflowConfigurationException;
import com.emberone.workflow.spi.ApproverOrg;
import com.emberone.workflow.spi.ParticipantSpec;
import com.emberone.workflow.spi.WorkflowApproverSlotRoles;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Materialises the participant set when a workflow is opened (Snapshot principle, Abschnitt 8). */
@Service
@RequiredArgsConstructor
public class WorkflowResolverService {

    private final WorkflowTypeMappingService mappingService;

    static boolean mayAutoClaimUserApprover(
            Long openedByUserId, boolean selfApproveAllowed, Long principalId) {
        if (principalId == null) {
            return false;
        }
        if (!principalId.equals(openedByUserId)) {
            return true;
        }
        return selfApproveAllowed;
    }

    /**
     * The org approver slots of a request as org id to slot role, in this order: the context org
     * ({@code CONTEXT}), the handler's dynamic orgs (their own roles), then the extra orgs from the
     * tenant settings ({@code ADDITIONAL}). One org is one slot: the first role wins, so an org
     * that is also the context org keeps the {@code CONTEXT} settings. When the extra orgs are
     * exclusive, only they decide, and the context org among them is still {@code CONTEXT}.
     */
    static Map<Long, String> resolveApproverOrganizationIds(
            Long contextOrganizationId,
            List<Long> staticOrganizationIds,
            boolean staticApproversExclusive,
            List<ApproverOrg> dynamicOrganizations) {
        Map<Long, String> approverOrgs = new LinkedHashMap<>();
        boolean hasStaticOrgs = staticOrganizationIds != null && !staticOrganizationIds.isEmpty();
        if (staticApproversExclusive && hasStaticOrgs) {
            staticOrganizationIds.forEach(orgId ->
                    approverOrgs.putIfAbsent(
                            orgId,
                            orgId.equals(contextOrganizationId)
                                    ? WorkflowApproverSlotRoles.CONTEXT
                                    : WorkflowApproverSlotRoles.ADDITIONAL));
            return approverOrgs;
        }
        approverOrgs.putIfAbsent(contextOrganizationId, WorkflowApproverSlotRoles.CONTEXT);
        if (dynamicOrganizations != null) {
            for (ApproverOrg dynamicOrg : dynamicOrganizations) {
                approverOrgs.putIfAbsent(dynamicOrg.organizationId(), dynamicOrg.slotRole());
            }
        }
        if (hasStaticOrgs) {
            staticOrganizationIds.forEach(
                    orgId -> approverOrgs.putIfAbsent(orgId, WorkflowApproverSlotRoles.ADDITIONAL));
        }
        return approverOrgs;
    }

    private static boolean isOrgGroupApprover(WorkflowParticipant participant) {
        return participant.isApprover()
                && participant.getPrincipalType() == PrincipalType.ORGANIZATION_GROUP
                && participant.getOrganizationId() != null;
    }

    public void resolveParticipants(
            Workflow workflow,
            Long openedByUserId,
            Long workflowTypeId,
            Long contextOrganizationId,
            List<Long> staticOrganizationIds,
            boolean staticApproversExclusive,
            boolean selfApproveAllowed,
            List<ApproverOrg> dynamicOrganizations,
            List<ParticipantSpec> additionalParticipants) {
        Long tenantId = workflow.getTenantId();
        Set<String> usedSlotKeys = new LinkedHashSet<>();

        Map<Long, String> approverOrgs =
                resolveApproverOrganizationIds(
                        contextOrganizationId,
                        staticOrganizationIds,
                        staticApproversExclusive,
                        dynamicOrganizations);

        approverOrgs.forEach((orgId, slotRole) -> {
            String slotKey = SlotKeys.forApproverOrg(orgId);
            registerSlotKey(usedSlotKeys, slotKey);
            workflow.getParticipants()
                    .add(
                            buildOrgApproverParticipant(
                                    workflow, tenantId, workflowTypeId, orgId, slotRole));
        });

        if (additionalParticipants != null) {
            for (ParticipantSpec spec : additionalParticipants) {
                String slotKey = SlotKeys.forAdditional(spec);
                registerSlotKey(usedSlotKeys, slotKey);
                workflow.getParticipants()
                        .add(
                                buildAdditionalParticipant(
                                        workflow,
                                        tenantId,
                                        openedByUserId,
                                        selfApproveAllowed,
                                        spec,
                                        slotKey));
            }
        }
    }

    /**
     * Adds missing org approver slots, drops obsolete pending ones and updates the role of existing
     * ones so open instances match current routing. Pending slots whose role changed are returned
     * so the caller can apply the settings of the new role (group, auto-approve).
     */
    public OrgApproverSlotSyncResult syncOrgApproverSlots(
            Workflow workflow,
            Long workflowTypeId,
            Long contextOrganizationId,
            List<Long> staticOrganizationIds,
            boolean staticApproversExclusive,
            List<ApproverOrg> dynamicOrganizations) {
        Map<Long, String> expectedOrgs =
                resolveApproverOrganizationIds(
                        contextOrganizationId,
                        staticOrganizationIds,
                        staticApproversExclusive,
                        dynamicOrganizations);
        Set<String> expectedSlotKeys = new LinkedHashSet<>();
        for (Long orgId : expectedOrgs.keySet()) {
            expectedSlotKeys.add(SlotKeys.forApproverOrg(orgId));
        }

        boolean changed = false;
        List<WorkflowParticipant> removedPending = new ArrayList<>();
        List<WorkflowParticipant> reroledPending = new ArrayList<>();
        for (WorkflowParticipant participant : new ArrayList<>(workflow.getParticipants())) {
            if (!isOrgGroupApprover(participant)) {
                continue;
            }
            if (expectedSlotKeys.contains(participant.getSlotKey())) {
                continue;
            }
            if (participant.getSlotStatus() != ParticipantSlotStatus.PENDING) {
                continue;
            }
            workflow.getParticipants().remove(participant);
            removedPending.add(participant);
            changed = true;
        }

        for (Map.Entry<Long, String> expected : expectedOrgs.entrySet()) {
            Long orgId = expected.getKey();
            String slotRole = expected.getValue();
            String slotKey = SlotKeys.forApproverOrg(orgId);
            Optional<WorkflowParticipant> existing =
                    workflow.getParticipants().stream()
                            .filter(participant -> slotKey.equals(participant.getSlotKey()))
                            .findFirst();
            if (existing.isPresent()) {
                WorkflowParticipant participant = existing.get();
                if (!slotRole.equals(participant.getSlotRole())) {
                    participant.setSlotRole(slotRole);
                    if (participant.getSlotStatus() == ParticipantSlotStatus.PENDING) {
                        reroledPending.add(participant);
                    }
                    changed = true;
                }
                continue;
            }
            workflow.getParticipants()
                    .add(
                            buildOrgApproverParticipant(
                                    workflow,
                                    workflow.getTenantId(),
                                    workflowTypeId,
                                    orgId,
                                    slotRole));
            changed = true;
        }

        return new OrgApproverSlotSyncResult(changed, removedPending, reroledPending);
    }

    /**
     * Sets the role of the existing org approver slots to the one the current routing expects (for
     * instances opened before slot roles existed). Does not add, remove or decide slots.
     *
     * @return the number of slots whose role changed
     */
    public int applySlotRoles(
            Workflow workflow,
            Long contextOrganizationId,
            List<Long> staticOrganizationIds,
            boolean staticApproversExclusive,
            List<ApproverOrg> dynamicOrganizations) {
        Map<Long, String> expectedOrgs =
                resolveApproverOrganizationIds(
                        contextOrganizationId,
                        staticOrganizationIds,
                        staticApproversExclusive,
                        dynamicOrganizations);
        int updated = 0;
        for (WorkflowParticipant participant : workflow.getParticipants()) {
            if (!isOrgGroupApprover(participant)) {
                continue;
            }
            String expectedRole = expectedOrgs.get(participant.getOrganizationId());
            if (expectedRole != null && !expectedRole.equals(participant.getSlotRole())) {
                participant.setSlotRole(expectedRole);
                updated++;
            }
        }
        return updated;
    }

    private WorkflowParticipant buildOrgApproverParticipant(
            Workflow workflow, Long tenantId, Long workflowTypeId, Long orgId, String slotRole) {
        WorkflowTypeMappingSlot slot = mappingService.requireSlot(orgId, workflowTypeId, slotRole);
        return WorkflowParticipant.builder()
                .tenantId(tenantId)
                .workflow(workflow)
                .role(ParticipantRole.APPROVER)
                .requirement(ParticipantRequirement.MANDATORY)
                .principalType(PrincipalType.ORGANIZATION_GROUP)
                .principalId(slot.getOrganizationGroupId())
                .organizationId(orgId)
                .slotKey(SlotKeys.forApproverOrg(orgId))
                .slotRole(slotRole)
                .slotStatus(
                        slot.isAutoApprove()
                                ? ParticipantSlotStatus.APPROVED
                                : ParticipantSlotStatus.PENDING)
                .build();
    }

    private WorkflowParticipant buildAdditionalParticipant(
            Workflow workflow,
            Long tenantId,
            Long openedByUserId,
            boolean selfApproveAllowed,
            ParticipantSpec spec,
            String slotKey) {
        WorkflowParticipant.WorkflowParticipantBuilder builder =
                WorkflowParticipant.builder()
                        .tenantId(tenantId)
                        .workflow(workflow)
                        .role(spec.role())
                        .requirement(ParticipantRequirement.MANDATORY)
                        .principalType(spec.principalType())
                        .principalId(spec.principalId())
                        .organizationId(spec.organizationId())
                        .slotKey(slotKey);

        if (spec.role() == ParticipantRole.APPROVER) {
            builder.slotStatus(ParticipantSlotStatus.PENDING);
            if (spec.principalType() == PrincipalType.USER
                    && mayAutoClaimUserApprover(
                            openedByUserId, selfApproveAllowed, spec.principalId())) {
                builder.claimedByUserId(spec.principalId());
                builder.claimedAt(Instant.now());
            }
        }
        return builder.build();
    }

    private void registerSlotKey(Set<String> usedSlotKeys, String slotKey) {
        if (!usedSlotKeys.add(slotKey)) {
            throw new WorkflowConfigurationException(
                    "Duplicate workflow slot key on opening: " + slotKey);
        }
    }

    public record OrgApproverSlotSyncResult(
            boolean changed,
            List<WorkflowParticipant> removedPendingParticipants,
            List<WorkflowParticipant> reroledPendingParticipants) {}
}
