/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.admin.management.list.AdminManagementActivationListFilterOptions;
import com.emberone.management.domain.ManagementActivationStatus;
import com.emberone.management.dto.ManagementActivationLogItemDto;
import com.emberone.management.exception.ManagementActivationNotFoundException;
import com.emberone.management.list.ManagementActivationListFilter;
import com.emberone.management.service.ManagementActivationLogService;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.web.PageDto;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.testsupport.JwtTestSupport;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminManagementActivationController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminManagementActivationControllerSecurityTest {

    private static final String PATH = "/api/v1/auth/admin/management-activations";

    @Autowired MockMvc mockMvc;
    @MockitoBean ManagementActivationLogService logService;
    @MockitoBean AdminManagementActivationListFilterOptions filterOptions;

    private void stubEmptyPage() {
        when(logService.listForAdmin(any(), any()))
                .thenReturn(
                        PageDto.<ManagementActivationLogItemDto>builder()
                                .content(List.of())
                                .build());
    }

    @Test
    void getRequiresAuthenticationAndMapsMissingEntryToNotFound() throws Exception {
        mockMvc.perform(get(PATH + "/5")).andExpect(status().isUnauthorized());

        when(logService.getForAdmin(5L)).thenThrow(new ManagementActivationNotFoundException(5L));
        mockMvc.perform(
                        get(PATH + "/5")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(PATH + "/list-filter-options")).andExpect(status().isUnauthorized());
    }

    @Test
    void tenantAndGlobalAdminsCanList() throws Exception {
        stubEmptyPage();

        mockMvc.perform(
                        get(PATH)
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
        mockMvc.perform(
                        get(PATH)
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.GLOBAL_ADMIN, null, null)))
                .andExpect(status().isOk());
    }

    @Test
    void organizationUserIsForbidden() throws Exception {
        mockMvc.perform(get(PATH).with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 1L)))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        get(PATH + "/list-filter-options")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void passesFiltersAndRejectsInvalidStatus() throws Exception {
        stubEmptyPage();

        mockMvc.perform(
                        get(PATH)
                                .param("tenantId", "7")
                                .param("status", "ACTIVE")
                                .param("organizationId", "3")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.GLOBAL_ADMIN, null, null)))
                .andExpect(status().isOk());
        verify(logService)
                .listForAdmin(
                        any(),
                        eq(
                                new ManagementActivationListFilter(
                                        List.of(7L),
                                        List.of(ManagementActivationStatus.ACTIVE),
                                        List.of(3L),
                                        List.of())));

        mockMvc.perform(
                        get(PATH)
                                .param("status", "NOPE")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminsCanLoadFilterOptions() throws Exception {
        when(filterOptions.filterOptions()).thenReturn(new ListFilterOptionsDto(Map.of()));

        mockMvc.perform(
                        get(PATH + "/list-filter-options")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
    }
}
