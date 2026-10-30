/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.user.dto.UserPageDto;
import com.emberone.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import(SecurityWebMvcTestConfig.class)
class UserControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserService userService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void organizationUsersRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/users/organizations/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void organizationUsersForbiddenWithoutPermission() throws Exception {
        when(coreSecurityService.hasUserReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/users/organizations/10")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void organizationUsersAllowedWithPermission() throws Exception {
        when(coreSecurityService.hasUserReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(userService.listUsersForOrganization(eq(SecurityTestFixtures.ORG_ID), any()))
                .thenReturn(UserPageDto.builder().build());

        mockMvc.perform(
                        get("/api/v1/auth/users/organizations/10")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
