/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.organization.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.admin.organization.service.AdminOrganizationService;
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

@WebMvcTest(AdminOrganizationController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminOrganizationChildrenControllerSecurityTest {

    private static final String CHILDREN = "/api/v1/auth/admin/tenants/7/organizations/10/children";

    @Autowired MockMvc mockMvc;

    @MockitoBean AdminOrganizationService adminOrganizationService;

    @Test
    void childrenRequireAuthentication() throws Exception {
        mockMvc.perform(get(CHILDREN)).andExpect(status().isUnauthorized());
    }

    @Test
    void regularUserCannotReadChildren() throws Exception {
        var user = JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 1L);
        mockMvc.perform(get(CHILDREN).with(user)).andExpect(status().isForbidden());
    }

    @Test
    void otherTenantAdminCannotReadChildren() throws Exception {
        var foreign = JwtTestSupport.jwtForRole(EmberOneAppRole.TENANT_ADMIN, 8L, 1L);
        mockMvc.perform(get(CHILDREN).with(foreign)).andExpect(status().isForbidden());
    }

    @Test
    void tenantAdminReceivesPlainList() throws Exception {
        when(adminOrganizationService.getChildOrganizations(7L, 10L)).thenReturn(List.of());
        var admin = JwtTestSupport.jwtForRole(EmberOneAppRole.TENANT_ADMIN, 7L, 1L);

        mockMvc.perform(get(CHILDREN).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
