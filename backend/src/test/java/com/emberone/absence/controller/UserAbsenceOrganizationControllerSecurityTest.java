/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.absence.list.AbsenceOrganizationListFilterOptions;
import com.emberone.absence.service.UserAbsenceService;
import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserAbsenceOrganizationController.class)
@Import(SecurityWebMvcTestConfig.class)
class UserAbsenceOrganizationControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserAbsenceService userAbsenceService;

    @MockitoBean AbsenceOrganizationListFilterOptions organizationListFilterOptions;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void activeAbsencesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/organizations/10/user-absences/active"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void activeAbsencesForbiddenWithoutPermission() throws Exception {
        when(coreSecurityService.hasAbsenceReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/user-absences/active")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void activeAbsencesAllowedWithPermission() throws Exception {
        when(coreSecurityService.hasAbsenceReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(userAbsenceService.listActiveAbsencesForOrganization(
                        SecurityTestFixtures.ORG_ID,
                        new com.emberone.platform.web.list.ListQuery(0, null, null, null),
                        com.emberone.absence.list.AbsenceListFilter.parseOrganizationActiveList(
                                null, null, null)))
                .thenReturn(com.emberone.absence.dto.UserAbsencePageDto.builder().build());

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/user-absences/active")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
