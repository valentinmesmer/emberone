/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.permission.domain.PermissionType;
import com.emberone.workflow.onboardingsubmission.OnboardingSubmissionWorkflowOpenAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurityServiceFacadeTest {

    private static final Long ORG_ID = 20L;
    private static final Long USER_ID = 10L;

    @Mock private TokenService tokenService;
    @Mock private AdminAccessService adminAccessService;
    @Mock private OrganizationAccessService organizationAccessService;
    @Mock private WorkflowOpenAccess workflowOpenAccess;
    @Mock private WorkflowInstanceAccess workflowInstanceAccess;
    @Mock private OnboardingSubmissionWorkflowOpenAccess onboardingSubmissionWorkflowOpenAccess;

    @InjectMocks private SecurityService service;

    @Test
    void adminMethodsDelegate() {
        when(adminAccessService.isGlobalAdmin()).thenReturn(true);
        when(adminAccessService.canAccessTenant(7L)).thenReturn(true);
        when(adminAccessService.isSelfServiceUser()).thenReturn(true);

        assertThat(service.isGlobalAdmin()).isTrue();
        assertThat(service.canAccessTenant(7L)).isTrue();
        assertThat(service.isSelfServiceUser()).isTrue();
    }

    @Test
    void organizationMethodsDelegate() {
        when(organizationAccessService.isActiveOrganizationMember(ORG_ID)).thenReturn(true);
        when(organizationAccessService.hasOrganizationGroupReadPermission(8L)).thenReturn(true);
        when(organizationAccessService.hasOrganizationPermission(ORG_ID, PermissionType.USER_READ))
                .thenReturn(true);
        when(organizationAccessService.hasEventCategoryReadPermission(ORG_ID, "service"))
                .thenReturn(true);
        when(organizationAccessService.hasUserWritePermissionForMembership(1L)).thenReturn(true);

        assertThat(service.isActiveOrganizationMember(ORG_ID)).isTrue();
        assertThat(service.hasOrganizationGroupReadPermission(8L)).isTrue();
        assertThat(service.hasUserReadPermission(ORG_ID)).isTrue();
        assertThat(service.hasEventCategoryReadPermission(ORG_ID, "service")).isTrue();
        assertThat(service.hasUserWritePermissionForMembership(1L)).isTrue();
    }

    @Test
    void workflowOpenDelegate() {
        when(workflowOpenAccess.canBeginSelfServiceWorkflow(5L)).thenReturn(true);

        assertThat(service.canBeginSelfServiceWorkflow(5L)).isTrue();
        assertThat(service.canUploadWorkflowDraftAttachment(5L)).isTrue();
    }

    @Test
    void onboardingSubmissionDelegate() {
        when(onboardingSubmissionWorkflowOpenAccess.canSubmit(5L)).thenReturn(true);

        assertThat(service.canSubmitOnboardingInvitation(5L)).isTrue();
    }

    @Test
    void workflowInstanceMethodsDelegateUsingCurrentUser() {
        when(tokenService.getUserId()).thenReturn(USER_ID);
        when(workflowInstanceAccess.canView(1L, USER_ID)).thenReturn(true);

        assertThat(service.canView(1L)).isTrue();
        service.canDecide(1L, 2L);
        service.canClaim(1L, 2L);
        service.canUnclaim(1L, 2L);
        service.canWithdrawApproval(1L, 2L);
        service.canCancel(1L);
        service.canComment(1L);
        service.canUpdateDocumentPromotions(1L);

        verify(workflowInstanceAccess).canView(1L, USER_ID);
        verify(workflowInstanceAccess).canDecide(1L, 2L, USER_ID);
        verify(workflowInstanceAccess).canClaim(1L, 2L, USER_ID);
        verify(workflowInstanceAccess).canUnclaim(1L, 2L, USER_ID);
        verify(workflowInstanceAccess).canWithdrawApproval(1L, 2L, USER_ID);
        verify(workflowInstanceAccess).canCancel(1L, USER_ID);
        verify(workflowInstanceAccess).canComment(1L, USER_ID);
        verify(workflowInstanceAccess).canUpdateDocumentPromotions(1L, USER_ID);
    }

    @Test
    void permissionConvenienceMethodsDelegateWithCorrectPermissionType() {
        service.hasUserWritePermission(ORG_ID);
        service.hasAppointmentReadPermission(ORG_ID);
        service.hasDocumentReadPermission(ORG_ID);
        service.hasCertificationReadPermission(ORG_ID);

        verify(organizationAccessService)
                .hasOrganizationPermission(ORG_ID, PermissionType.USER_WRITE);
        verify(organizationAccessService)
                .hasOrganizationPermission(ORG_ID, PermissionType.APPOINTMENT_READ);
        verify(organizationAccessService)
                .hasOrganizationPermission(ORG_ID, PermissionType.DOCUMENT_READ);
        verify(organizationAccessService)
                .hasOrganizationPermission(ORG_ID, PermissionType.CERTIFICATION_READ);
    }
}
