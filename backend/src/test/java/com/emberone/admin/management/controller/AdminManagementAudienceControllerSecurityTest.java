/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.management.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.management.dto.ManagementAudienceSettingsDto;
import com.emberone.management.service.ManagementAudienceService;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.testsupport.JwtTestSupport;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminManagementAudienceController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminManagementAudienceControllerSecurityTest {

    private static final String PATH =
            "/api/v1/auth/admin/tenants/7/organizations/10/management-audiences";

    @Autowired MockMvc mockMvc;
    @MockitoBean ManagementAudienceService managementAudienceService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    void tenantAdminCanReadOnlyOwnTenant() throws Exception {
        when(managementAudienceService.getSettings(7L, 10L))
                .thenReturn(ManagementAudienceSettingsDto.builder().build());

        mockMvc.perform(
                        get(PATH)
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/8/organizations/10/management-audiences")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void organizationUserWithoutTenantAdminRoleIsForbidden() throws Exception {
        mockMvc.perform(get(PATH).with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 1L)))
                .andExpect(status().isForbidden());
    }
}
