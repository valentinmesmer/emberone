/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowParticipant;
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

    private final WorkflowDefinitionService definitionService;
    private final WorkflowHandlerRegistry handlerRegistry;

    /**
     * For an approver slot of an open or closed workflow instance. Slots without an organization
     * (user principals) use {@link WorkflowHandler#resolveInstanceOrganization(Workflow)} so
     * payload-backed types resolve the same organization as access checks.
     */
    @Transactional(readOnly = true)
    public boolean isAllowed(Workflow workflow, Long slotOrganizationId) {
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
            if (organizationId == null) {
                return false;
            }
            return definitionService
                    .resolveEffectiveDefinition(workflow.getWorkflowTypeId(), organizationId)
                    .isSelfApproveAllowed();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** The opener needs a claimed approver slot whose organization allows self-approve. */
    @Transactional(readOnly = true)
    public boolean blocksInputChangeRequest(Workflow workflow, Long userId) {
        return userId.equals(workflow.getOpenedByUserId())
                && workflow.getParticipants().stream()
                        .filter(WorkflowParticipant::isApprover)
                        .filter(participant -> userId.equals(participant.getClaimedByUserId()))
                        .noneMatch(
                                participant ->
                                        isAllowed(workflow, participant.getOrganizationId()));
    }

    /**
     * Pre-open helper when no {@link Workflow} exists yet. Only valid for types whose security
     * context organization does not depend on a payload.
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
            return definitionService
                    .resolveEffectiveDefinition(workflowTypeId, contextOrgId)
                    .isSelfApproveAllowed();
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
