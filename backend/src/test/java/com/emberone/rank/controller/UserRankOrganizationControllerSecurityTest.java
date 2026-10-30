/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.dutyfunction.service.DutyFunctionResolutionService;
import com.emberone.platform.security.SecurityService;
import com.emberone.rank.dto.UserRankSelfDto;
import com.emberone.rank.service.UserRankService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserRankOrganizationController.class)
@Import(SecurityWebMvcTestConfig.class)
class UserRankOrganizationControllerSecurityTest {

    private static final String SET_RANK_PAYLOAD =
            """
        {"tenantRankId":1,"startDate":"2026-01-01","reason":"test"}
        """;

    @Autowired MockMvc mockMvc;

    @MockitoBean UserRankService userRankService;
    @MockitoBean DutyFunctionResolutionService dutyFunctionResolutionService;
    @MockitoBean UserService userService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void getUserRankForbiddenWithoutRankRead() throws Exception {
        when(coreSecurityService.hasRankReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/users/42/rank")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserRankAllowedWithRankRead() throws Exception {
        when(coreSecurityService.hasRankReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        doNothing()
                .when(userService)
                .assertUserBelongsToOrganization(42L, SecurityTestFixtures.ORG_ID);
        when(userRankService.getSelfRank(42L)).thenReturn(UserRankSelfDto.builder().build());

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/users/42/rank")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void setUserRankForbiddenWithoutRankWrite() throws Exception {
        when(coreSecurityService.hasRankWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/auth/organizations/10/users/42/rank")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(SET_RANK_PAYLOAD))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserFunctionsAllowedWithRankRead() throws Exception {
        when(coreSecurityService.hasRankReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        doNothing()
                .when(userService)
                .assertUserBelongsToOrganization(42L, SecurityTestFixtures.ORG_ID);
        when(dutyFunctionResolutionService.resolveForUserInOrganization(
                        42L, SecurityTestFixtures.ORG_ID))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/users/42/functions")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
