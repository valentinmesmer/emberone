/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.onboarding.dto.SelfOnboardingInvitationDetailDto;
import com.emberone.onboarding.service.OnboardingInvitationProgressService;
import com.emberone.onboarding.service.OnboardingInvitationService;
import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OnboardingInvitationOrganizationController.class)
@Import(SecurityWebMvcTestConfig.class)
class OnboardingInvitationOrganizationControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean OnboardingInvitationService onboardingInvitationService;
    @MockitoBean OnboardingInvitationProgressService onboardingInvitationProgressService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void pendingInvitationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/organizations/10/onboarding-invitations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pendingInvitationsForbiddenWithoutPermission() throws Exception {
        when(coreSecurityService.hasUserReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/onboarding-invitations")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void pendingInvitationsAllowedWithPermission() throws Exception {
        when(coreSecurityService.hasUserReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(onboardingInvitationService.listForOrganization(
                        eq(SecurityTestFixtures.ORG_ID), any()))
                .thenReturn(
                        com.emberone.onboarding.dto.OnboardingInvitationPageDto.builder().build());

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/onboarding-invitations")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void invitationDetailRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/organizations/10/onboarding-invitations/5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invitationDetailForbiddenWithoutPermission() throws Exception {
        when(coreSecurityService.hasUserReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/onboarding-invitations/5")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void invitationDetailAllowedWithPermission() throws Exception {
        when(coreSecurityService.hasUserReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(onboardingInvitationProgressService.getDetailForOrganization(
                        SecurityTestFixtures.ORG_ID, 5L))
                .thenReturn(SelfOnboardingInvitationDetailDto.builder().id(5L).build());

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/onboarding-invitations/5")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void signedDocumentDownloadRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        get(
                                "/api/v1/auth/organizations/10/onboarding-invitations/5/documents/DSGVO/file"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signedDocumentDownloadForbiddenWithoutPermission() throws Exception {
        when(coreSecurityService.hasUserReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/onboarding-invitations/5/documents/DSGVO/file")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void signedDocumentDownloadAllowedWithPermission() throws Exception {
        when(coreSecurityService.hasUserReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(onboardingInvitationProgressService.downloadSignedDocumentForOrganization(
                        SecurityTestFixtures.ORG_ID, 5L, "DSGVO"))
                .thenReturn(
                        new OnboardingInvitationProgressService.StoredUpload(
                                new java.io.ByteArrayInputStream(new byte[] {1}),
                                "signed.pdf",
                                "application/pdf"));

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/onboarding-invitations/5/documents/DSGVO/file")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
