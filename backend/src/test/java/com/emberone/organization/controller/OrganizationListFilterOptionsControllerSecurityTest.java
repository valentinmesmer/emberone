/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.SecurityService;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.platform.web.list.options.ListFilterOptionsService;
import com.emberone.testsupport.JwtTestSupport;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizationListFilterOptionsController.class)
@Import(SecurityWebMvcTestConfig.class)
class OrganizationListFilterOptionsControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ListFilterOptionsService listFilterOptionsService;
    @MockitoBean SecurityService coreSecurityService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/list-filter-options")
                                .param("include", "organizationId"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberWithUserReadCanLoadOptions() throws Exception {
        when(coreSecurityService.hasUserReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(listFilterOptionsService.resolve(any(), any()))
                .thenReturn(new ListFilterOptionsDto(Map.of()));

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/list-filter-options")
                                .param("include", "organizationId")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 1L)))
                .andExpect(status().isOk());
    }

    @Test
    void forbiddenWithoutUserRead() throws Exception {
        when(coreSecurityService.hasUserReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/list-filter-options")
                                .param("include", "organizationId")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 1L)))
                .andExpect(status().isForbidden());
    }
}
