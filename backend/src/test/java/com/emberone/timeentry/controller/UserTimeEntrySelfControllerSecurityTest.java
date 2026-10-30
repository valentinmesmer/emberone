/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.timeentry.dto.UserTimeEntryPageDto;
import com.emberone.timeentry.list.TimeEntrySelfListFilterOptions;
import com.emberone.timeentry.service.UserTimeEntryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserTimeEntrySelfController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class UserTimeEntrySelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserTimeEntryService userTimeEntryService;
    @MockitoBean TimeEntrySelfListFilterOptions selfListFilterOptions;

    @Test
    void timeEntriesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/time-entries")).andExpect(status().isUnauthorized());
    }

    @Test
    void timeEntriesAllowedForTenantUser() throws Exception {
        when(userTimeEntryService.listTimeEntriesForUser(
                        eq(SecurityTestFixtures.USER_ID), any(), any()))
                .thenReturn(UserTimeEntryPageDto.builder().build());

        mockMvc.perform(get("/api/v1/self/time-entries").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());

        verify(userTimeEntryService)
                .listTimeEntriesForUser(eq(SecurityTestFixtures.USER_ID), any(), any());
    }
}
