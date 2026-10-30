/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.onboarding.dto.SelfOnboardingInvitationDetailDto;
import com.emberone.onboarding.service.OnboardingInvitationProgressService;
import com.emberone.onboarding.service.OnboardingInvitationService;
import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OnboardingInvitationSelfController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class OnboardingInvitationSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean OnboardingInvitationService onboardingInvitationService;
    @MockitoBean OnboardingInvitationProgressService onboardingInvitationProgressService;

    @Test
    void pendingInvitationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/onboarding-invitations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pendingInvitationsAllowedForTenantUser() throws Exception {
        when(onboardingInvitationService.listAllForUser(SecurityTestFixtures.USER_ID))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/self/onboarding-invitations")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void invitationDetailRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/onboarding-invitations/3"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invitationDetailAllowedForTenantUser() throws Exception {
        when(onboardingInvitationProgressService.getDetail(SecurityTestFixtures.USER_ID, 3L))
                .thenReturn(SelfOnboardingInvitationDetailDto.builder().id(3L).build());

        mockMvc.perform(
                        get("/api/v1/self/onboarding-invitations/3")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void signedDocumentDownloadRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/onboarding-invitations/3/documents/DSGVO/file"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signedDocumentDownloadAllowedForTenantUser() throws Exception {
        when(onboardingInvitationProgressService.downloadSignedDocument(
                        SecurityTestFixtures.USER_ID, 3L, "DSGVO"))
                .thenReturn(
                        new OnboardingInvitationProgressService.StoredUpload(
                                new java.io.ByteArrayInputStream(new byte[] {1}),
                                "signed.pdf",
                                "application/pdf"));

        mockMvc.perform(
                        get("/api/v1/self/onboarding-invitations/3/documents/DSGVO/file")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void saveStepRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        put("/api/v1/self/onboarding-invitations/3/steps/EMERGENCY_CONTACTS")
                                .contentType("application/json")
                                .content("{\"payload\":{}}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteDrivingLicenseAttachmentRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        delete(
                                "/api/v1/self/onboarding-invitations/3/steps/DRIVING_LICENSE/attachment"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteDrivingLicenseAttachmentAllowedForTenantUser() throws Exception {
        when(onboardingInvitationProgressService.deleteDrivingLicenseAttachment(
                        SecurityTestFixtures.USER_ID, 3L, 0))
                .thenReturn(SelfOnboardingInvitationDetailDto.builder().id(3L).build());

        mockMvc.perform(
                        delete(
                                        "/api/v1/self/onboarding-invitations/3/steps/DRIVING_LICENSE/attachment")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void deleteRecordAttachmentRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        delete(
                                "/api/v1/self/onboarding-invitations/3/steps/CERTIFICATIONS/records/rec-1/attachment"))
                .andExpect(status().isUnauthorized());
    }
}
