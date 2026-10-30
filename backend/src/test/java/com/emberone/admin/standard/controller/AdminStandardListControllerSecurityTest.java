/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.standard.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.admin.standard.service.AdminStandardService;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.testsupport.JwtTestSupport;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminStandardController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminStandardListControllerSecurityTest {

    private static final String ASSIGNMENTS = "/api/v1/auth/admin/tenants/7/standards/assignments";
    private static final String OVERRIDES =
            "/api/v1/auth/admin/tenants/7/standards/organization-overrides";

    @Autowired MockMvc mockMvc;

    @MockitoBean AdminStandardService adminStandardService;

    @Test
    void listsRequireAuthentication() throws Exception {
        mockMvc.perform(get(ASSIGNMENTS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(OVERRIDES)).andExpect(status().isUnauthorized());
    }

    @Test
    void regularUserCannotReadLists() throws Exception {
        var user = JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 1L);
        mockMvc.perform(get(ASSIGNMENTS).with(user)).andExpect(status().isForbidden());
        mockMvc.perform(get(OVERRIDES).with(user)).andExpect(status().isForbidden());
    }

    @Test
    void otherTenantAdminCannotReadLists() throws Exception {
        var foreign = JwtTestSupport.jwtForRole(EmberOneAppRole.TENANT_ADMIN, 8L, 1L);
        mockMvc.perform(get(ASSIGNMENTS).with(foreign)).andExpect(status().isForbidden());
        mockMvc.perform(get(OVERRIDES).with(foreign)).andExpect(status().isForbidden());
    }

    @Test
    void tenantAdminReceivesPlainLists() throws Exception {
        when(adminStandardService.listAssignments(7L)).thenReturn(List.of());
        when(adminStandardService.listOrganizationOverrides(7L)).thenReturn(List.of());
        var admin = JwtTestSupport.jwtForRole(EmberOneAppRole.TENANT_ADMIN, 7L, 1L);

        mockMvc.perform(get(ASSIGNMENTS).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mockMvc.perform(get(OVERRIDES).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
