/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.onboarding.domain.OnboardingInvitation;
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

@WebMvcTest(OnboardingInvitationUserOrganizationController.class)
@Import(SecurityWebMvcTestConfig.class)
class OnboardingInvitationUserOrganizationControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean OnboardingInvitationService onboardingInvitationService;
    @MockitoBean OnboardingInvitationProgressService onboardingInvitationProgressService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void userOnboardingInvitationRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/organizations/10/users/3/onboarding-invitation"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userOnboardingInvitationForbiddenWithoutPermission() throws Exception {
        when(coreSecurityService.hasUserReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/users/3/onboarding-invitation")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void userOnboardingInvitationAllowedWithPermission() throws Exception {
        when(coreSecurityService.hasUserReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(onboardingInvitationService.requireInvitationForOrganizationUser(
                        SecurityTestFixtures.ORG_ID, 3L))
                .thenReturn(OnboardingInvitation.builder().id(5L).build());
        when(onboardingInvitationProgressService.getDetailForOrganization(
                        SecurityTestFixtures.ORG_ID, 5L))
                .thenReturn(SelfOnboardingInvitationDetailDto.builder().id(5L).build());

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/users/3/onboarding-invitation")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
