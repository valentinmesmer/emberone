/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.retention.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.admin.retention.dto.TenantRetentionProfileDto;
import com.emberone.admin.retention.service.AdminRetentionService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminRetentionController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminRetentionControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AdminRetentionService adminRetentionService;

    @Test
    void retentionRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin/tenants/7/retention"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void retentionForbiddenForForeignTenant() throws Exception {
        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/8/retention")
                                .with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void retentionAllowedForOwnTenant() throws Exception {
        when(adminRetentionService.getProfileForTenant(7L))
                .thenReturn(
                        TenantRetentionProfileDto.builder()
                                .profileKey("BY")
                                .periods(List.of())
                                .build());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/7/retention")
                                .with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isOk());
    }
}
