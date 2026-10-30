/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.onboarding.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.admin.onboarding.service.AdminOnboardingInvitationService;
import com.emberone.onboarding.dto.OnboardingStepsAvailableDto;
import com.emberone.onboarding.dto.SelfOnboardingInvitationDetailDto;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.testsupport.JwtTestSupport;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(AdminOnboardingInvitationController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminOnboardingInvitationControllerSecurityTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AdminOnboardingInvitationService adminOnboardingInvitationService;

    @Test
    void onboardingAvailableRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin/tenants/7/organizations/10/onboarding-available"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tenantAdminCanAccessOnlyOwnTenantOnboarding() throws Exception {
        when(adminOnboardingInvitationService.getOnboardingAvailable(7L, 10L))
                .thenReturn(OnboardingStepsAvailableDto.builder().available(true).build());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/7/organizations/10/onboarding-available")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/8/organizations/10/onboarding-available")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void startRecordingAllowedForTenantAdmin() throws Exception {
        when(adminOnboardingInvitationService.startRecording(7L, 10L, 20L))
                .thenReturn(SelfOnboardingInvitationDetailDto.builder().id(3L).build());

        mockMvc.perform(
                        post("/api/v1/auth/admin/tenants/7/organizations/10/users/20/onboarding-invitations")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isCreated());
    }

    @Test
    void recordingAvailableRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        get(
                                "/api/v1/auth/admin/tenants/7/organizations/10/users/20/onboarding-recording-available"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tenantAdminCanAccessOnlyOwnTenantRecordingAvailable() throws Exception {
        when(adminOnboardingInvitationService.getRecordingAvailable(7L, 10L, 20L))
                .thenReturn(OnboardingStepsAvailableDto.builder().available(true).build());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/7/organizations/10/users/20/onboarding-recording-available")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/8/organizations/10/users/20/onboarding-recording-available")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void organizationEntryDocumentUploadRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        post(
                                "/api/v1/auth/admin/tenants/7/organizations/10/onboarding-invitations/3/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/dd"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tenantAdminCanUploadOrganizationEntryDocumentInOwnTenant() throws Exception {
        when(adminOnboardingInvitationService.uploadOrganizationSignedDocument(
                        eq(7L), eq(10L), eq(3L), eq("dd"), any()))
                .thenReturn(SelfOnboardingInvitationDetailDto.builder().id(3L).build());

        mockMvc.perform(
                        MockMvcRequestBuilders.multipart(
                                        "/api/v1/auth/admin/tenants/7/organizations/10/onboarding-invitations/3/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/dd")
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "signed.pdf",
                                                "application/pdf",
                                                "data".getBytes()))
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
    }

    @Test
    void rankAttachmentUploadRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        post(
                                "/api/v1/auth/admin/tenants/7/organizations/10/onboarding-invitations/3/steps/RANK/attachment"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tenantAdminCanUploadRankAttachmentInOwnTenant() throws Exception {
        when(adminOnboardingInvitationService.uploadRankAttachment(eq(7L), eq(10L), eq(3L), any()))
                .thenReturn(SelfOnboardingInvitationDetailDto.builder().id(3L).build());

        mockMvc.perform(
                        MockMvcRequestBuilders.multipart(
                                        "/api/v1/auth/admin/tenants/7/organizations/10/onboarding-invitations/3/steps/RANK/attachment")
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "proof.pdf",
                                                "application/pdf",
                                                "data".getBytes()))
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
    }

    @Test
    void tenantAdminCannotUploadRankAttachmentInOtherTenant() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.multipart(
                                        "/api/v1/auth/admin/tenants/8/organizations/10/onboarding-invitations/3/steps/RANK/attachment")
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "proof.pdf",
                                                "application/pdf",
                                                "data".getBytes()))
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void rankAttachmentDeleteRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        delete(
                                "/api/v1/auth/admin/tenants/7/organizations/10/onboarding-invitations/3/steps/RANK/attachment"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tenantAdminCanDeleteRankAttachmentInOwnTenant() throws Exception {
        when(adminOnboardingInvitationService.deleteRankAttachment(7L, 10L, 3L, 0))
                .thenReturn(SelfOnboardingInvitationDetailDto.builder().id(3L).build());

        mockMvc.perform(
                        delete(
                                        "/api/v1/auth/admin/tenants/7/organizations/10/onboarding-invitations/3/steps/RANK/attachment")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
    }

    @Test
    void rankAttachmentDownloadRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        get(
                                "/api/v1/auth/admin/tenants/7/organizations/10/onboarding-invitations/3/steps/RANK/attachment"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getDetailForUserRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        get(
                                "/api/v1/auth/admin/tenants/7/organizations/10/users/20/onboarding-invitation"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tenantAdminCanAccessOnlyOwnTenantDetailForUser() throws Exception {
        when(adminOnboardingInvitationService.getDetailForUser(7L, 10L, 20L))
                .thenReturn(SelfOnboardingInvitationDetailDto.builder().id(3L).build());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/7/organizations/10/users/20/onboarding-invitation")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/8/organizations/10/users/20/onboarding-invitation")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isForbidden());
    }
}
