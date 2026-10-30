/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.organizationgroup.dto.OrganizationGroupDto;
import com.emberone.organizationgroup.service.OrganizationGroupService;
import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.userorganizationgroupmembership.service.UserOrganizationGroupMembershipService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizationGroupController.class)
@Import(SecurityWebMvcTestConfig.class)
class OrganizationGroupControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean OrganizationGroupService organizationGroupService;
    @MockitoBean UserOrganizationGroupMembershipService userOrganizationGroupMembershipService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void groupDetailRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/organizations/groups/5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void groupDetailForbiddenWithoutPermission() throws Exception {
        when(coreSecurityService.hasOrganizationGroupReadPermission(SecurityTestFixtures.GROUP_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/groups/5")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void groupDetailAllowedWithPermission() throws Exception {
        when(coreSecurityService.hasOrganizationGroupReadPermission(SecurityTestFixtures.GROUP_ID))
                .thenReturn(true);
        when(organizationGroupService.getGroupById(SecurityTestFixtures.GROUP_ID))
                .thenReturn(
                        OrganizationGroupDto.builder().id(SecurityTestFixtures.GROUP_ID).build());

        mockMvc.perform(
                        get("/api/v1/auth/organizations/groups/5")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
