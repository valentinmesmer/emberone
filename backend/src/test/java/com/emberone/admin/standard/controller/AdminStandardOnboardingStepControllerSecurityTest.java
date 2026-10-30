/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.standard.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.admin.onboarding.dto.OnboardingStepTypesResponse;
import com.emberone.admin.onboarding.dto.OnboardingStepsResponse;
import com.emberone.admin.standard.service.AdminStandardService;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.testsupport.JwtTestSupport;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminStandardController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminStandardOnboardingStepControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AdminStandardService adminStandardService;

    @Test
    void onboardingStepsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin/tenants/7/standards/1/onboarding-steps"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void regularUserCannotReadOnboardingSteps() throws Exception {
        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/7/standards/1/onboarding-steps")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantAdminCanReadOnboardingSteps() throws Exception {
        when(adminStandardService.getOnboardingSteps(7L, 1L))
                .thenReturn(OnboardingStepsResponse.builder().steps(java.util.List.of()).build());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/7/standards/1/onboarding-steps")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
    }

    @Test
    void tenantAdminCanReplaceOnboardingSteps() throws Exception {
        when(adminStandardService.replaceOnboardingSteps(
                        org.mockito.ArgumentMatchers.eq(7L),
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(OnboardingStepsResponse.builder().steps(java.util.List.of()).build());

        mockMvc.perform(
                        put("/api/v1/auth/admin/tenants/7/standards/1/onboarding-steps")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"steps\":[]}"))
                .andExpect(status().isOk());
    }

    @Test
    void tenantAdminCanListOnboardingStepTypes() throws Exception {
        when(adminStandardService.listOnboardingStepTypes(7L))
                .thenReturn(
                        OnboardingStepTypesResponse.builder()
                                .stepTypes(java.util.List.of())
                                .build());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/7/standards/onboarding-step-types")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
    }
}
