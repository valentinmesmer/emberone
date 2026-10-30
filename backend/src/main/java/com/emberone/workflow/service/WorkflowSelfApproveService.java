/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves whether self-approve is allowed from the current org workflow definition (live, not
 * snapshotted at open time).
 */
@Service
@RequiredArgsConstructor
public class WorkflowSelfApproveService {

    private final WorkflowDefinitionService definitionService;
    private final WorkflowHandlerRegistry handlerRegistry;

    /**
     * For an open or closed workflow instance. Uses {@link
     * WorkflowHandler#resolveInstanceOrganization(Workflow)} so payload-backed types (e.g. org user
     * write) resolve the same organization as access checks and reconciliation.
     */
    @Transactional(readOnly = true)
    public boolean isAllowed(Workflow workflow) {
        if (workflow == null
                || workflow.getWorkflowTypeId() == null
                || workflow.getTenantId() == null) {
            return false;
        }
        try {
            WorkflowHandler<?> handler = handlerRegistry.require(workflow.getWorkflowTypeId());
            Long contextOrgId = handler.resolveInstanceOrganization(workflow);
            if (contextOrgId == null) {
                return false;
            }
            return definitionService
                    .resolveEffectiveDefinition(workflow.getWorkflowTypeId(), contextOrgId)
                    .isSelfApproveAllowed();
        } catch (RuntimeException ex) {
            return false;
        }
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
