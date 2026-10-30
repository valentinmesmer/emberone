/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.organization.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.admin.organization.dto.AdminOrganizationDetailDto;
import com.emberone.admin.organization.service.AdminOrganizationService;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.testsupport.JwtTestSupport;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminOrganizationController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminOrganizationControllerSecurityTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AdminOrganizationService adminOrganizationService;

    @Test
    void organizationAdministrationRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin/tenants/7/organizations/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tenantAdminCanAccessOnlyOwnTenantOrganizations() throws Exception {
        when(adminOrganizationService.getOrganization(7L, 10L))
                .thenReturn(AdminOrganizationDetailDto.builder().id(10L).build());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/7/organizations/10")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/8/organizations/10")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isForbidden());
    }
}
