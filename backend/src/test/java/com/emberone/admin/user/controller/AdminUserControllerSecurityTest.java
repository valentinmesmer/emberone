/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.admin.user.dto.TenantUserListItemDto;
import com.emberone.admin.user.list.AdminUserListFilter;
import com.emberone.admin.user.list.AdminUserListFilterOptions;
import com.emberone.admin.user.service.AdminUserService;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.web.PageDto;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.testsupport.JwtTestSupport;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminUserController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminUserControllerSecurityTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AdminUserService adminUserService;
    @MockitoBean AdminUserListFilterOptions adminUserListFilterOptions;

    @Test
    void userAdministrationRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin/users")).andExpect(status().isUnauthorized());

        when(adminUserService.listUsers(any(), any()))
                .thenReturn(PageDto.<TenantUserListItemDto>builder().content(List.of()).build());
        mockMvc.perform(
                        get("/api/v1/auth/admin/users")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
    }

    @Test
    void listUsersPassesFiltersAndRejectsInvalidAppRole() throws Exception {
        when(adminUserService.listUsers(any(), any()))
                .thenReturn(PageDto.<TenantUserListItemDto>builder().content(List.of()).build());
        mockMvc.perform(
                        get("/api/v1/auth/admin/users")
                                .param("tenantId", "7")
                                .param("organizationId", "3")
                                .param("appRole", "TENANT_ADMIN")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.GLOBAL_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
        verify(adminUserService)
                .listUsers(
                        any(),
                        eq(
                                new AdminUserListFilter(
                                        List.of(7L),
                                        List.of(3L),
                                        List.of(EmberOneAppRole.TENANT_ADMIN))));

        mockMvc.perform(
                        get("/api/v1/auth/admin/users")
                                .param("appRole", "NOPE")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.GLOBAL_ADMIN, 7L, 1L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listFilterOptionsRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin/users/list-filter-options"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        get("/api/v1/auth/admin/users/list-filter-options")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 1L)))
                .andExpect(status().isForbidden());

        when(adminUserListFilterOptions.filterOptions())
                .thenReturn(new ListFilterOptionsDto(java.util.Map.of()));
        mockMvc.perform(
                        get("/api/v1/auth/admin/users/list-filter-options")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
    }

    @Test
    void getUserRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin/users/5")).andExpect(status().isUnauthorized());

        mockMvc.perform(
                        get("/api/v1/auth/admin/users/5")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 1L)))
                .andExpect(status().isForbidden());

        when(adminUserService.getUser(5L))
                .thenReturn(TenantUserListItemDto.builder().userId(5L).build());
        mockMvc.perform(
                        get("/api/v1/auth/admin/users/5")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
        verify(adminUserService).getUser(5L);
    }
}
