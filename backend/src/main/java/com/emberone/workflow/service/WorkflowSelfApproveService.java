/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.service;

import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowParticipant;
import com.emberone.workflow.spi.WorkflowApproverSlotRoles;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves whether self-approve is allowed from the current workflow definition of the approver
 * slot's organization (live, not snapshotted at open time). A workflow can have slots in several
 * organizations, each deciding for its own slot.
 */
@Service
@RequiredArgsConstructor
public class WorkflowSelfApproveService {

    private final WorkflowTypeMappingService mappingService;
    private final WorkflowHandlerRegistry handlerRegistry;

    /**
     * For an approver slot of an open or closed workflow instance: the {@code self_approve_allowed}
     * setting of the slot's role. Slots without an organization (user principals) use {@link
     * WorkflowHandler#resolveInstanceOrganization(Workflow)} so payload-backed types resolve the
     * same organization as access checks.
     */
    @Transactional(readOnly = true)
    public boolean isAllowed(Workflow workflow, WorkflowParticipant participant) {
        return isAllowed(workflow, participant.getOrganizationId(), participant.getSlotRole());
    }

    /**
     * Same as {@link #isAllowed(Workflow, WorkflowParticipant)} for a slot given by org and role.
     */
    @Transactional(readOnly = true)
    public boolean isAllowed(Workflow workflow, Long slotOrganizationId, String slotRole) {
        if (workflow == null
                || workflow.getWorkflowTypeId() == null
                || workflow.getTenantId() == null) {
            return false;
        }
        try {
            Long organizationId =
                    slotOrganizationId != null
                            ? slotOrganizationId
                            : handlerRegistry
                                    .require(workflow.getWorkflowTypeId())
                                    .resolveInstanceOrganization(workflow);
            return isAllowedForSlot(organizationId, workflow.getWorkflowTypeId(), slotRole);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * The {@code self_approve_allowed} setting of a role in an organization's mapping, for callers
     * that know the slot but have no persisted participant (opening, reconcile). Fails closed.
     */
    @Transactional(readOnly = true)
    public boolean isAllowedForSlot(Long organizationId, Long workflowTypeId, String slotRole) {
        if (organizationId == null || workflowTypeId == null) {
            return false;
        }
        try {
            return mappingService
                    .requireSlot(organizationId, workflowTypeId, roleOrContext(slotRole))
                    .isSelfApproveAllowed();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** The opener needs a claimed approver slot whose role allows self-approve. */
    @Transactional(readOnly = true)
    public boolean blocksInputChangeRequest(Workflow workflow, Long userId) {
        return userId.equals(workflow.getOpenedByUserId())
                && workflow.getParticipants().stream()
                        .filter(WorkflowParticipant::isApprover)
                        .filter(participant -> userId.equals(participant.getClaimedByUserId()))
                        .noneMatch(participant -> isAllowed(workflow, participant));
    }

    /**
     * Pre-open helper when no {@link Workflow} exists yet: the setting of the {@code CONTEXT} role.
     * Only valid for types whose security context organization does not depend on a payload.
     */
    @Transactional(readOnly = true)
    public boolean isAllowed(Long workflowTypeId, Long openedByUserId, Long tenantId) {
        try {
            WorkflowHandler<?> handler = handlerRegistry.require(workflowTypeId);
            Long contextOrgId =
                    handler.resolveSecurityContextOrganization(openedByUserId, tenantId);
            if (contextOrgId == null) {
                return false;
            }
            return isAllowedForSlot(
                    contextOrgId, workflowTypeId, WorkflowApproverSlotRoles.CONTEXT);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static String roleOrContext(String slotRole) {
        return slotRole != null ? slotRole : WorkflowApproverSlotRoles.CONTEXT;
    }
}
