/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.event.service.EventAttendanceService;
import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.user.dto.UserDto;
import com.emberone.user.service.SelfMembershipHistoryService;
import com.emberone.user.service.ServiceHoursTotalService;
import com.emberone.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserSelfController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class UserSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserService userService;
    @MockitoBean EventAttendanceService eventAttendanceService;
    @MockitoBean ServiceHoursTotalService serviceHoursTotalService;
    @MockitoBean SelfMembershipHistoryService selfMembershipHistoryService;

    @Test
    void selfUserDataRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self")).andExpect(status().isUnauthorized());
    }

    @Test
    void selfUserDataAllowedForTenantUser() throws Exception {
        when(userService.getUserById(SecurityTestFixtures.USER_ID))
                .thenReturn(UserDto.builder().id(SecurityTestFixtures.USER_ID).build());

        mockMvc.perform(get("/api/v1/self").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
