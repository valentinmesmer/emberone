/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.drivinglicense.service.DrivingLicenseCatalogService;
import com.emberone.drivinglicense.service.DrivingLicenseRecordService;
import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserDrivingLicenseSelfController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class UserDrivingLicenseSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean DrivingLicenseRecordService recordService;
    @MockitoBean DrivingLicenseCatalogService catalogService;

    @Test
    void selfAvailableDefinitionsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/driving-license/available-definitions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void selfAvailableDefinitionsAllowedForTenantUser() throws Exception {
        when(catalogService.listAvailableDefinitionsForUser(SecurityTestFixtures.USER_ID))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/self/driving-license/available-definitions")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
