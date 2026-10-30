/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.dutyfunction.service.DutyFunctionResolutionService;
import com.emberone.platform.security.SecurityService;
import com.emberone.rank.dto.OrganizationRankMemberPageDto;
import com.emberone.rank.service.TenantRankCatalogService;
import com.emberone.rank.service.UserRankService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizationRankController.class)
@Import(SecurityWebMvcTestConfig.class)
class OrganizationRankControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserRankService userRankService;
    @MockitoBean TenantRankCatalogService tenantRankCatalogService;
    @MockitoBean DutyFunctionResolutionService dutyFunctionResolutionService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void listMembersRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/organizations/10/ranks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listMembersForbiddenWithoutRankRead() throws Exception {
        when(coreSecurityService.hasRankReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/ranks")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listMembersAllowedWithRankRead() throws Exception {
        when(coreSecurityService.hasRankReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(userRankService.listOrganizationMembers(
                        SecurityTestFixtures.ORG_ID,
                        new com.emberone.platform.web.list.ListQuery(0, null, null, null)))
                .thenReturn(OrganizationRankMemberPageDto.builder().content(List.of()).build());

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/ranks")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void availableRanksForbiddenWithoutRankWrite() throws Exception {
        when(coreSecurityService.hasRankWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/ranks/available-ranks")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }
}
