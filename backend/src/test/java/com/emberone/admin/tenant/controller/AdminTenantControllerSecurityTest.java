/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.admin.tenant.dto.TenantAdminDetailDto;
import com.emberone.admin.tenant.dto.TenantDto;
import com.emberone.admin.tenant.list.AdminTenantListFilterOptions;
import com.emberone.admin.tenant.service.AdminTenantPurgeService;
import com.emberone.admin.tenant.service.AdminTenantService;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.web.PageDto;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.testsupport.JwtTestSupport;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminTenantController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminTenantControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AdminTenantService tenantAdminService;
    @MockitoBean AdminTenantPurgeService adminTenantPurgeService;
    @MockitoBean AdminTenantListFilterOptions adminTenantListFilterOptions;

    @Test
    void listFilterOptionsSameAccessAsListTenants() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin/tenants/list-filter-options"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/list-filter-options")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 1L)))
                .andExpect(status().isForbidden());

        when(adminTenantListFilterOptions.filterOptions())
                .thenReturn(new ListFilterOptionsDto(java.util.Map.of()));
        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/list-filter-options")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
    }

    @Test
    void adminTenantsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin/tenants")).andExpect(status().isUnauthorized());
    }

    @Test
    void regularUserCannotListTenants() throws Exception {
        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantAdminCanListTenants() throws Exception {
        when(tenantAdminService.listTenants(any(), any()))
                .thenReturn(PageDto.<TenantDto>builder().build());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
    }

    @Test
    void globalAdminCanListTenants() throws Exception {
        when(tenantAdminService.listTenants(any(), any()))
                .thenReturn(PageDto.<TenantDto>builder().build());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.GLOBAL_ADMIN, null, 1L)))
                .andExpect(status().isOk());
    }

    @Test
    void tenantAdminCanAccessOwnTenantDetail() throws Exception {
        when(tenantAdminService.getTenantDetail(7L))
                .thenReturn(TenantAdminDetailDto.builder().id(7L).build());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/7")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
    }

    @Test
    void tenantAdminCannotAccessForeignTenantDetail() throws Exception {
        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/8")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantAdminCannotPurgeTenant() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/admin/tenants/7/purge")
                                .contentType("application/json")
                                .content("{\"confirmationName\":\"Demo\"}")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantAdminCannotSyncBaseline() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/admin/tenants/7/baseline-sync")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void globalAdminCanSyncBaseline() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/admin/tenants/7/baseline-sync")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.GLOBAL_ADMIN, null, 1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    void globalAdminCanPurgeTenant() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/admin/tenants/7/purge")
                                .contentType("application/json")
                                .content("{\"confirmationName\":\"Demo\"}")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.GLOBAL_ADMIN, null, 1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    void globalAdminCanAccessAnyTenantDetail() throws Exception {
        when(tenantAdminService.getTenantDetail(8L))
                .thenReturn(TenantAdminDetailDto.builder().id(8L).build());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/8")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.GLOBAL_ADMIN, null, 1L)))
                .andExpect(status().isOk());
    }
}
