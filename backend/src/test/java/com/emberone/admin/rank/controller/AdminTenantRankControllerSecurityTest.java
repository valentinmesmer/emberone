/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.rank.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.SecurityService;
import com.emberone.rank.service.TenantRankCatalogService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminTenantRankController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminTenantRankControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean TenantRankCatalogService catalogService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void listCatalogRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin/tenants/7/ranks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCatalogForbiddenWithoutTenantAccess() throws Exception {
        when(coreSecurityService.canAccessTenant(SecurityTestFixtures.TENANT_ID)).thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/7/ranks")
                                .with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listCatalogAllowedWithTenantAccess() throws Exception {
        when(coreSecurityService.canAccessTenant(SecurityTestFixtures.TENANT_ID)).thenReturn(true);
        when(catalogService.listCatalog(SecurityTestFixtures.TENANT_ID)).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/7/ranks")
                                .with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isOk());
    }
}
