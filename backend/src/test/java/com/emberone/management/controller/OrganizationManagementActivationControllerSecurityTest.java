/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.management.dto.ManagementActivationLogItemDto;
import com.emberone.management.service.ManagementActivationLogService;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.SecurityService;
import com.emberone.platform.web.PageDto;
import com.emberone.testsupport.JwtTestSupport;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizationManagementActivationController.class)
@Import(SecurityWebMvcTestConfig.class)
class OrganizationManagementActivationControllerSecurityTest {

    private static final String PATH = "/api/v1/auth/organizations/10/management-activations";

    @Autowired MockMvc mockMvc;
    @MockitoBean ManagementActivationLogService logService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    void organizationReadCanListTheLogOfItsOrganization() throws Exception {
        when(coreSecurityService.hasOrganizationPermission(10L, PermissionType.ORGANIZATION_READ))
                .thenReturn(true);
        when(logService.listForTarget(eq(10L), any(), any()))
                .thenReturn(
                        PageDto.<ManagementActivationLogItemDto>builder()
                                .content(List.of())
                                .build());

        mockMvc.perform(get(PATH).with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 1L)))
                .andExpect(status().isOk());
    }

    @Test
    void forbiddenWithoutOrganizationRead() throws Exception {
        when(coreSecurityService.hasOrganizationPermission(10L, PermissionType.ORGANIZATION_READ))
                .thenReturn(false);

        mockMvc.perform(get(PATH).with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 1L)))
                .andExpect(status().isForbidden());
    }
}
