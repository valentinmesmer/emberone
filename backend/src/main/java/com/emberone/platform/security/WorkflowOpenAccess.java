/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import com.emberone.permission.domain.PermissionType;
import com.emberone.rank.service.UserRankService;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.exception.WorkflowConfigurationException;
import com.emberone.workflow.service.WorkflowDefinitionService;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Pre-open authorization for starting a self-service workflow of a given type. */
@Service
@RequiredArgsConstructor
public class WorkflowOpenAccess {

    private final TokenService tokenService;
    private final OrganizationAccessService organizationAccessService;
    private final WorkflowHandlerRegistry workflowHandlerRegistry;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final UserOrganizationMembershipRepository userOrganizationMembershipRepository;
    private final UserRankService userRankService;

    @Transactional(readOnly = true)
    public boolean canBeginSelfServiceWorkflow(long workflowTypeId) {
        Long userId = tokenService.getUserId();
        Long tenantId = tokenService.getTenantId();
        if (userId == null || tenantId == null) {
            return false;
        }
        if (workflowTypeId == WorkflowType.RANK_REQUEST.getId()
                && userRankService.hasOpenRank(userId)) {
            return false;
        }
        WorkflowHandler<?> handler = workflowHandlerRegistry.require(workflowTypeId);
        if (!userOrganizationMembershipRepository.existsActiveByUser_Id(userId, Instant.now())
                && !handler.mayOpenWithoutActiveOrganizationMembership()) {
            return false;
        }
        if (handler.requiresOpenerOrganizationMembership()
                && userOrganizationMembershipRepository
                        .findActiveByUser_IdAndMainTrue(userId, Instant.now())
                        .isEmpty()) {
            return false;
        }
        Long contextOrgId;
        try {
            contextOrgId = handler.resolveSecurityContextOrganization(userId, tenantId);
        } catch (RuntimeException ex) {
            return false;
        }
        if (contextOrgId == null) {
            return false;
        }
        WorkflowDefinition definition;
        try {
            definition =
                    workflowDefinitionService.resolveEffectiveDefinition(
                            workflowTypeId, contextOrgId);
        } catch (WorkflowConfigurationException ex) {
            return false;
        }
        if (!definition.isEnabled()) {
            return false;
        }
        var requiredPermission = handler.requiredPermissionToOpen();
        if (requiredPermission.isEmpty()) {
            return true;
        }
        if (tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())) {
            return true;
        }
        PermissionType required = requiredPermission.get();
        @SuppressWarnings("unchecked")
        WorkflowHandler<Object> typed = (WorkflowHandler<Object>) handler;
        WorkflowOpenContext<Object> ctx = new WorkflowOpenContext<>(userId, tenantId, null);
        Long permissionOrgId = typed.resolvePermissionCheckOrganization(ctx, contextOrgId);
        if (permissionOrgId == null) {
            return false;
        }
        return organizationAccessService.hasOrganizationPermission(permissionOrgId, required);
    }
}
