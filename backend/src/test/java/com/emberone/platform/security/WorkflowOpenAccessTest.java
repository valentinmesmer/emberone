/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.emberone.permission.domain.PermissionType;
import com.emberone.rank.service.UserRankService;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.exception.WorkflowConfigurationException;
import com.emberone.workflow.service.WorkflowDefinitionService;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowOpenAccessTest {

    private static final long WORKFLOW_TYPE_ID = 5L;
    private static final Long USER_ID = 10L;
    private static final Long TENANT_ID = 20L;
    private static final Long CONTEXT_ORG_ID = 30L;
    private static final Long PERMISSION_ORG_ID = 31L;

    @Mock private TokenService tokenService;
    @Mock private OrganizationAccessService organizationAccessService;
    @Mock private WorkflowHandlerRegistry workflowHandlerRegistry;
    @Mock private WorkflowDefinitionService workflowDefinitionService;
    @Mock private RequestScopedOpenerMemberships openerMemberships;
    @Mock private UserRankService userRankService;
    @Mock private WorkflowHandler<Object> handler;

    @InjectMocks private WorkflowOpenAccess service;

    @Test
    void canBeginSelfServiceWorkflow_requiresUserAndTenantClaims() {
        when(tokenService.getUserId()).thenReturn(null);
        assertThat(service.canBeginSelfServiceWorkflow(WORKFLOW_TYPE_ID)).isFalse();

        when(tokenService.getUserId()).thenReturn(USER_ID);
        when(tokenService.getTenantId()).thenReturn(null);
        assertThat(service.canBeginSelfServiceWorkflow(WORKFLOW_TYPE_ID)).isFalse();
    }

    @Test
    void canBeginSelfServiceWorkflow_rejectsResolutionErrorsMissingContextAndDisabledDefinition() {
        prepareTokenAndHandler();
        doThrow(new IllegalStateException("invalid context"))
                .when(handler)
                .resolveSecurityContextOrganization(USER_ID, TENANT_ID);
        assertThat(service.canBeginSelfServiceWorkflow(WORKFLOW_TYPE_ID)).isFalse();

        doReturn(null).when(handler).resolveSecurityContextOrganization(USER_ID, TENANT_ID);
        assertThat(service.canBeginSelfServiceWorkflow(WORKFLOW_TYPE_ID)).isFalse();

        doReturn(CONTEXT_ORG_ID)
                .when(handler)
                .resolveSecurityContextOrganization(USER_ID, TENANT_ID);
        when(workflowDefinitionService.resolveEffectiveDefinition(WORKFLOW_TYPE_ID, CONTEXT_ORG_ID))
                .thenReturn(WorkflowDefinition.builder().enabled(false).build());
        assertThat(service.canBeginSelfServiceWorkflow(WORKFLOW_TYPE_ID)).isFalse();
    }

    @Test
    void canBeginSelfServiceWorkflow_rejectsMissingWorkflowDefinition() {
        prepareTokenAndHandler();
        when(handler.resolveSecurityContextOrganization(USER_ID, TENANT_ID))
                .thenReturn(CONTEXT_ORG_ID);
        when(workflowDefinitionService.resolveEffectiveDefinition(WORKFLOW_TYPE_ID, CONTEXT_ORG_ID))
                .thenThrow(new WorkflowConfigurationException("No workflow definition"));

        assertThat(service.canBeginSelfServiceWorkflow(WORKFLOW_TYPE_ID)).isFalse();
    }

    @Test
    void canBeginSelfServiceWorkflow_allowsEnabledWorkflowWithoutRequiredPermission() {
        prepareEnabledWorkflow();
        when(handler.requiredPermissionToOpen()).thenReturn(Optional.empty());

        assertThat(service.canBeginSelfServiceWorkflow(WORKFLOW_TYPE_ID)).isTrue();
    }

    @Test
    void canBeginSelfServiceWorkflow_allowsGlobalAdminWhenPermissionIsRequired() {
        prepareEnabledWorkflow();
        when(handler.requiredPermissionToOpen()).thenReturn(Optional.of(PermissionType.USER_WRITE));
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(true);

        assertThat(service.canBeginSelfServiceWorkflow(WORKFLOW_TYPE_ID)).isTrue();
    }

    @Test
    void canBeginSelfServiceWorkflow_checksResolvedPermissionOrganization() {
        prepareEnabledWorkflow();
        when(handler.requiredPermissionToOpen()).thenReturn(Optional.of(PermissionType.USER_WRITE));
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(handler.resolvePermissionCheckOrganization(
                        org.mockito.ArgumentMatchers.<WorkflowOpenContext<Object>>any(),
                        eq(CONTEXT_ORG_ID)))
                .thenReturn(PERMISSION_ORG_ID);
        when(organizationAccessService.hasOrganizationPermission(
                        PERMISSION_ORG_ID, PermissionType.USER_WRITE))
                .thenReturn(true);

        assertThat(service.canBeginSelfServiceWorkflow(WORKFLOW_TYPE_ID)).isTrue();
        verify(organizationAccessService)
                .hasOrganizationPermission(PERMISSION_ORG_ID, PermissionType.USER_WRITE);
    }

    @Test
    void canBeginSelfServiceWorkflow_rejectsWhenOpenerMembershipRequiredAndMissing() {
        prepareTokenAndHandler();
        when(handler.requiresOpenerOrganizationMembership()).thenReturn(true);
        when(openerMemberships.hasActiveMainMembership(USER_ID)).thenReturn(false);

        assertThat(service.canBeginSelfServiceWorkflow(WORKFLOW_TYPE_ID)).isFalse();
        verify(handler, never()).resolveSecurityContextOrganization(any(), any());
    }

    @Test
    void canBeginSelfServiceWorkflow_allowsWhenOpenerMembershipNotRequiredAndMissing() {
        prepareEnabledWorkflow();
        when(handler.requiresOpenerOrganizationMembership()).thenReturn(false);
        when(handler.requiredPermissionToOpen()).thenReturn(Optional.empty());

        assertThat(service.canBeginSelfServiceWorkflow(WORKFLOW_TYPE_ID)).isTrue();
        verify(openerMemberships, never()).hasActiveMainMembership(any());
    }

    @Test
    void canBeginSelfServiceWorkflow_continuesWhenOpenerMembershipRequiredAndPresent() {
        prepareEnabledWorkflow();
        when(handler.requiresOpenerOrganizationMembership()).thenReturn(true);
        when(openerMemberships.hasActiveMainMembership(USER_ID)).thenReturn(true);
        when(handler.requiredPermissionToOpen()).thenReturn(Optional.empty());

        assertThat(service.canBeginSelfServiceWorkflow(WORKFLOW_TYPE_ID)).isTrue();
    }

    @Test
    void canBeginSelfServiceWorkflow_rejectsWhenUserHasNoOrganizationMembership() {
        when(tokenService.getUserId()).thenReturn(USER_ID);
        when(tokenService.getTenantId()).thenReturn(TENANT_ID);
        doReturn(handler).when(workflowHandlerRegistry).require(WORKFLOW_TYPE_ID);
        when(openerMemberships.hasActiveMembership(USER_ID)).thenReturn(false);
        when(handler.mayOpenWithoutActiveOrganizationMembership()).thenReturn(false);

        assertThat(service.canBeginSelfServiceWorkflow(WORKFLOW_TYPE_ID)).isFalse();
        verify(handler, never()).resolveSecurityContextOrganization(any(), any());
    }

    @Test
    void canBeginSelfServiceWorkflow_rejectsRankRequestWhenUserAlreadyHasOpenRank() {
        when(tokenService.getUserId()).thenReturn(USER_ID);
        when(tokenService.getTenantId()).thenReturn(TENANT_ID);
        when(userRankService.hasOpenRank(USER_ID)).thenReturn(true);

        assertThat(service.canBeginSelfServiceWorkflow(WorkflowType.RANK_REQUEST.getId()))
                .isFalse();
        verifyNoInteractions(workflowHandlerRegistry);
    }

    @Test
    void canBeginSelfServiceWorkflow_rejectsMissingPermissionOrganization() {
        prepareEnabledWorkflow();
        when(handler.requiredPermissionToOpen()).thenReturn(Optional.of(PermissionType.USER_WRITE));
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(handler.resolvePermissionCheckOrganization(
                        org.mockito.ArgumentMatchers.<WorkflowOpenContext<Object>>any(),
                        eq(CONTEXT_ORG_ID)))
                .thenReturn(null);

        assertThat(service.canBeginSelfServiceWorkflow(WORKFLOW_TYPE_ID)).isFalse();
    }

    private void prepareTokenAndHandler() {
        when(tokenService.getUserId()).thenReturn(USER_ID);
        when(tokenService.getTenantId()).thenReturn(TENANT_ID);
        doReturn(handler).when(workflowHandlerRegistry).require(WORKFLOW_TYPE_ID);
        when(openerMemberships.hasActiveMembership(USER_ID)).thenReturn(true);
    }

    private void prepareEnabledWorkflow() {
        prepareTokenAndHandler();
        when(handler.resolveSecurityContextOrganization(USER_ID, TENANT_ID))
                .thenReturn(CONTEXT_ORG_ID);
        when(workflowDefinitionService.resolveEffectiveDefinition(WORKFLOW_TYPE_ID, CONTEXT_ORG_ID))
                .thenReturn(WorkflowDefinition.builder().enabled(true).build());
    }
}
