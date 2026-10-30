/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.TokenService;
import com.emberone.rank.dto.UserRankSelfDto;
import com.emberone.rank.service.TenantRankCatalogService;
import com.emberone.rank.service.UserRankService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserRankSelfController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class UserRankSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserRankService userRankService;
    @MockitoBean TenantRankCatalogService tenantRankCatalogService;

    @Test
    void selfRankRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/rank")).andExpect(status().isUnauthorized());
    }

    @Test
    void selfRankAllowedForTenantUser() throws Exception {
        when(userRankService.getSelfRank(SecurityTestFixtures.USER_ID))
                .thenReturn(UserRankSelfDto.builder().build());

        mockMvc.perform(get("/api/v1/self/rank").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
