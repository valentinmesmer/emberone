/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userpermission.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.userpermission.dto.UserPermissionDto;
import com.emberone.userpermission.service.UserPermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SelfPermissionController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class SelfPermissionControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserPermissionService userPermissionService;

    @Test
    void selfPermissionsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/permissions")).andExpect(status().isUnauthorized());
    }

    @Test
    void selfPermissionsAllowedForTenantUser() throws Exception {
        when(userPermissionService.getUserPermissions(SecurityTestFixtures.USER_ID))
                .thenReturn(UserPermissionDto.builder().build());

        mockMvc.perform(get("/api/v1/self/permissions").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
