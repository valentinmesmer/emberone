/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationgroupmembership.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.userorganizationgroupmembership.dto.UserOrganizationGroupMembershipDto;
import com.emberone.userorganizationgroupmembership.service.UserOrganizationGroupMembershipService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserOrganizationGroupMembershipController.class)
@Import(SecurityWebMvcTestConfig.class)
class UserOrganizationGroupMembershipControllerSecurityTest {

    private static final String CREATE_BODY =
            """
        {"userId":42,"organizationGroupId":5,"startDate":"2024-01-01T00:00:00","membershipType":"MEMBER"}
        """;
    @Autowired MockMvc mockMvc;
    @MockitoBean UserOrganizationGroupMembershipService membershipService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void createMembershipRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/users/organization-group-memberships")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CREATE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createMembershipForbiddenWithoutPermission() throws Exception {
        when(coreSecurityService.hasUserWritePermissionForOrganizationGroup(
                        SecurityTestFixtures.GROUP_ID))
                .thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/auth/users/organization-group-memberships")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void createMembershipAllowedWithPermission() throws Exception {
        when(coreSecurityService.hasUserWritePermissionForOrganizationGroup(
                        SecurityTestFixtures.GROUP_ID))
                .thenReturn(true);
        when(membershipService.createMembership(any()))
                .thenReturn(UserOrganizationGroupMembershipDto.builder().id(1L).build());

        mockMvc.perform(
                        post("/api/v1/auth/users/organization-group-memberships")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CREATE_BODY))
                .andExpect(status().isCreated());
    }
}
