/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.absence.list.AbsenceOrganizationUserListFilterOptions;
import com.emberone.absence.service.UserAbsenceService;
import com.emberone.platform.security.SecurityService;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.user.service.UserService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserAbsenceUserOrganizationController.class)
@Import(SecurityWebMvcTestConfig.class)
class UserAbsenceUserOrganizationControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserAbsenceService userAbsenceService;

    @MockitoBean UserService userService;

    @MockitoBean AbsenceOrganizationUserListFilterOptions organizationUserListFilterOptions;

    @MockitoBean(name = "securityService")
    SecurityService securityService;

    @Test
    void listFilterOptionsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/organizations/10/users/42/absences/list-filter-options"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listFilterOptionsForbiddenWithoutPermission() throws Exception {
        when(securityService.hasAbsenceReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/users/42/absences/list-filter-options")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listFilterOptionsAllowedWithPermission() throws Exception {
        when(securityService.hasAbsenceReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        doNothing()
                .when(userService)
                .assertUserBelongsToOrganization(
                        SecurityTestFixtures.USER_ID, SecurityTestFixtures.ORG_ID);
        when(organizationUserListFilterOptions.forUserInOrganization(
                        SecurityTestFixtures.USER_ID, SecurityTestFixtures.ORG_ID))
                .thenReturn(new ListFilterOptionsDto(Map.of()));

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/users/42/absences/list-filter-options")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
