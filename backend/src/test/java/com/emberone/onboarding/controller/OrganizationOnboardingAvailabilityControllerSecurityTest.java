/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.onboarding.service.OnboardingInvitationReadinessService;
import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizationOnboardingAvailabilityController.class)
@Import(SecurityWebMvcTestConfig.class)
class OrganizationOnboardingAvailabilityControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean OnboardingInvitationReadinessService onboardingInvitationReadinessService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void onboardingAvailableRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/organizations/10/onboarding-available"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void onboardingAvailableForbiddenWithoutPermission() throws Exception {
        when(coreSecurityService.hasUserReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/onboarding-available")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void onboardingAvailableAllowedWithPermission() throws Exception {
        when(coreSecurityService.hasUserReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(onboardingInvitationReadinessService.hasConfiguredOnboardingSteps(10L))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/onboarding-available")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }
}
