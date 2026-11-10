/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.dutyfunction.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.dutyfunction.service.TenantDutyFunctionDefinitionService;
import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminTenantDutyFunctionController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminTenantDutyFunctionControllerSecurityTest {

    private static final String URL = "/api/v1/auth/admin/tenants/7/functions/KEY";

    @Autowired MockMvc mockMvc;

    @MockitoBean TenantDutyFunctionDefinitionService functionService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void getRequiresAuthentication() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void getForbiddenWithoutTenantAccess() throws Exception {
        when(coreSecurityService.canAccessTenant(SecurityTestFixtures.TENANT_ID)).thenReturn(false);

        mockMvc.perform(get(URL).with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllowedWithTenantAccess() throws Exception {
        when(coreSecurityService.canAccessTenant(SecurityTestFixtures.TENANT_ID)).thenReturn(true);

        mockMvc.perform(get(URL).with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isOk());
    }
}
