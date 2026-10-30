/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.absence.list.AbsenceSelfListFilterOptions;
import com.emberone.absence.service.UserAbsenceService;
import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserAbsenceSelfController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class UserAbsenceSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserAbsenceService userAbsenceService;
    @MockitoBean AbsenceSelfListFilterOptions selfListFilterOptions;

    @Test
    void selfAbsencesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/absences")).andExpect(status().isUnauthorized());
    }

    @Test
    void selfAbsencesAllowedForTenantUser() throws Exception {
        when(userAbsenceService.listAbsencesForUser(
                        SecurityTestFixtures.USER_ID,
                        new com.emberone.platform.web.list.ListQuery(0, null, null, null),
                        com.emberone.absence.list.AbsenceListFilter.empty()))
                .thenReturn(com.emberone.absence.dto.UserAbsencePageDto.builder().build());

        mockMvc.perform(get("/api/v1/self/absences").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
