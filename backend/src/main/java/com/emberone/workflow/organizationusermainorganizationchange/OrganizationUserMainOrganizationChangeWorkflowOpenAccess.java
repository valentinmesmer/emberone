/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.organizationusermainorganizationchange;

import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.exception.WorkflowConfigurationException;
import com.emberone.workflow.service.WorkflowDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationUserMainOrganizationChangeWorkflowOpenAccess {

    private final OrganizationAccessService organizationAccessService;
    private final WorkflowDefinitionService workflowDefinitionService;

    @Transactional(readOnly = true)
    public boolean canOpen(long organizationId, long targetUserId) {
        if (!organizationAccessService.canRequestMainOrganizationChangeForUser(
                organizationId, targetUserId)) {
            return false;
        }
        return hasEnabledWorkflowDefinition(organizationId);
    }

    private boolean hasEnabledWorkflowDefinition(long organizationId) {
        try {
            WorkflowDefinition definition =
                    workflowDefinitionService.resolveEffectiveDefinition(
                            WorkflowType.ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE.getId(),
                            organizationId);
            return definition.isEnabled();
        } catch (WorkflowConfigurationException ex) {
            return false;
        }
    }
}
