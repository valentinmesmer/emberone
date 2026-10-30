/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.workflow.domain.*;
import com.emberone.workflow.exception.WorkflowConfigurationException;
import com.emberone.workflow.spi.ParticipantSpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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

    static Set<Long> resolveApproverOrganizationIds(
            Long contextOrganizationId,
            List<Long> staticOrganizationIds,
            boolean staticApproversExclusive,
            List<Long> dynamicOrganizationIds) {
        Set<Long> approverOrgIds = new LinkedHashSet<>();
        boolean hasStaticOrgs = staticOrganizationIds != null && !staticOrganizationIds.isEmpty();
        if (staticApproversExclusive && hasStaticOrgs) {
            approverOrgIds.addAll(staticOrganizationIds);
        } else {
            approverOrgIds.add(contextOrganizationId);
            if (staticOrganizationIds != null) {
                approverOrgIds.addAll(staticOrganizationIds);
            }
            if (dynamicOrganizationIds != null) {
                approverOrgIds.addAll(dynamicOrganizationIds);
            }
        }
        return approverOrgIds;
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
            List<Long> dynamicOrganizationIds,
            List<ParticipantSpec> additionalParticipants) {
        Long tenantId = workflow.getTenantId();
        Set<String> usedSlotKeys = new LinkedHashSet<>();

        Set<Long> approverOrgIds =
                resolveApproverOrganizationIds(
                        contextOrganizationId,
                        staticOrganizationIds,
                        staticApproversExclusive,
                        dynamicOrganizationIds);

        for (Long orgId : approverOrgIds) {
            String slotKey = SlotKeys.forApproverOrg(orgId);
            registerSlotKey(usedSlotKeys, slotKey);
            workflow.getParticipants()
                    .add(buildOrgApproverParticipant(workflow, tenantId, workflowTypeId, orgId));
        }

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
     * Adds missing org approver slots and drops obsolete pending ones so open instances match
     * current routing.
     */
    public OrgApproverSlotSyncResult syncOrgApproverSlots(
            Workflow workflow,
            Long workflowTypeId,
            Long contextOrganizationId,
            List<Long> staticOrganizationIds,
            boolean staticApproversExclusive,
            List<Long> dynamicOrganizationIds) {
        Set<Long> expectedOrgIds =
                resolveApproverOrganizationIds(
                        contextOrganizationId,
                        staticOrganizationIds,
                        staticApproversExclusive,
                        dynamicOrganizationIds);
        Set<String> expectedSlotKeys = new LinkedHashSet<>();
        for (Long orgId : expectedOrgIds) {
            expectedSlotKeys.add(SlotKeys.forApproverOrg(orgId));
        }

        boolean changed = false;
        List<WorkflowParticipant> removedPending = new ArrayList<>();
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

        for (Long orgId : expectedOrgIds) {
            String slotKey = SlotKeys.forApproverOrg(orgId);
            boolean exists =
                    workflow.getParticipants().stream()
                            .anyMatch(participant -> slotKey.equals(participant.getSlotKey()));
            if (exists) {
                continue;
            }
            workflow.getParticipants()
                    .add(
                            buildOrgApproverParticipant(
                                    workflow, workflow.getTenantId(), workflowTypeId, orgId));
            changed = true;
        }

        return new OrgApproverSlotSyncResult(changed, removedPending);
    }

    private WorkflowParticipant buildOrgApproverParticipant(
            Workflow workflow, Long tenantId, Long workflowTypeId, Long orgId) {
        WorkflowTypeMapping mapping = mappingService.requireMapping(orgId, workflowTypeId);
        return WorkflowParticipant.builder()
                .tenantId(tenantId)
                .workflow(workflow)
                .role(ParticipantRole.APPROVER)
                .requirement(ParticipantRequirement.MANDATORY)
                .principalType(PrincipalType.ORGANIZATION_GROUP)
                .principalId(mapping.getOrganizationGroupId())
                .organizationId(orgId)
                .slotKey(SlotKeys.forApproverOrg(orgId))
                .slotStatus(
                        mapping.isAutoApprove()
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
            boolean changed, List<WorkflowParticipant> removedPendingParticipants) {}
}
