/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.admin.user.service.AdminUserService;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.testsupport.JwtTestSupport;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminTenantUserController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminTenantUserControllerSecurityTest {

    private static final String PATH = "/api/v1/auth/admin/tenants/7/users/99/deactivate";

    @Autowired MockMvc mockMvc;
    @MockitoBean AdminUserService adminUserService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post(PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    void tenantAdminDeactivatesInOwnTenantOnly() throws Exception {
        mockMvc.perform(
                        post(PATH)
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isNoContent());
        verify(adminUserService).deactivateUser(7L, 99L);

        mockMvc.perform(
                        post("/api/v1/auth/admin/tenants/8/users/99/deactivate")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void globalAdminDeactivatesInAnyTenant() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/admin/tenants/8/users/99/deactivate")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.GLOBAL_ADMIN, null, null)))
                .andExpect(status().isNoContent());
        verify(adminUserService).deactivateUser(8L, 99L);
    }

    @Test
    void plainUserIsForbidden() throws Exception {
        mockMvc.perform(post(PATH).with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 1L)))
                .andExpect(status().isForbidden());
    }

    private static final String APP_ROLE = "/api/v1/auth/admin/tenants/7/users/99/app-role";
    private static final String ROLE_BODY = "{\"appRole\":\"USER\"}";

    @Test
    void appRoleChangeIsTenantScoped() throws Exception {
        mockMvc.perform(
                        patch(APP_ROLE)
                                .contentType("application/json")
                                .content(ROLE_BODY)
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isNoContent());
        verify(adminUserService).updateUserAppRole(eq(7L), eq(99L), any());

        mockMvc.perform(
                        patch("/api/v1/auth/admin/tenants/8/users/99/app-role")
                                .contentType("application/json")
                                .content(ROLE_BODY)
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        patch(APP_ROLE)
                                .contentType("application/json")
                                .content(ROLE_BODY)
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 1L)))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        patch("/api/v1/auth/admin/tenants/8/users/99/app-role")
                                .contentType("application/json")
                                .content(ROLE_BODY)
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.GLOBAL_ADMIN, null, null)))
                .andExpect(status().isNoContent());
    }
}
