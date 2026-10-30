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

import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.timeentry.dto.UserTimeEntryPageDto;
import com.emberone.timeentry.service.UserTimeEntryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserTimeEntryOrganizationController.class)
@Import(SecurityWebMvcTestConfig.class)
class UserTimeEntryOrganizationControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserTimeEntryService userTimeEntryService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void timeEntriesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/organizations/10/user-time-entries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void timeEntriesForbiddenWithoutPermission() throws Exception {
        when(coreSecurityService.hasTimeEntryReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/user-time-entries")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void timeEntriesAllowedWithPermission() throws Exception {
        when(coreSecurityService.hasTimeEntryReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(userTimeEntryService.listTimeEntriesForOrganization(
                        eq(SecurityTestFixtures.ORG_ID), any(), any()))
                .thenReturn(UserTimeEntryPageDto.builder().build());

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/user-time-entries")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());

        verify(userTimeEntryService)
                .listTimeEntriesForOrganization(eq(SecurityTestFixtures.ORG_ID), any(), any());
    }
}
