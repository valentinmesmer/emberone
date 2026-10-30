/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.organization.dto.OrganizationDto;
import com.emberone.organizationgroup.service.OrganizationGroupService;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.SecurityService;
import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.JwtTestSupport;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.user.service.UserService;
import com.emberone.userorganizationmembership.dto.UserOrganizationMembershipDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizationSelfController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class OrganizationSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserService userService;
    @MockitoBean OrganizationGroupService organizationGroupService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void selfOrganizationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/organizations")).andExpect(status().isUnauthorized());
    }

    @Test
    void selfOrganizationsRejectUserTokenWithoutTenant() throws Exception {
        mockMvc.perform(
                        get("/api/v1/self/organizations")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, null, 42L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void selfOrganizationsAllowTenantUser() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(true);
        when(userService.getOrganizationMembershipsForUser(42L)).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/self/organizations")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 42L)))
                .andExpect(status().isOk());
    }

    @Test
    void selfOrganizationsAllowGlobalAdminWithoutTenant() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(true);
        when(userService.getOrganizationMembershipsForUser(99L))
                .thenReturn(
                        List.of(
                                UserOrganizationMembershipDto.builder()
                                        .id(1L)
                                        .userId(99L)
                                        .build()));

        mockMvc.perform(
                        get("/api/v1/self/organizations")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.GLOBAL_ADMIN, null, 99L)))
                .andExpect(status().isOk());
    }

    @Test
    void selfOrganizationByIdRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/organizations/10")).andExpect(status().isUnauthorized());
    }

    @Test
    void selfOrganizationByIdForbiddenWhenNotMember() throws Exception {
        when(coreSecurityService.isActiveOrganizationMember(10L)).thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/self/organizations/10")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 42L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void selfOrganizationByIdReturnsOrganizationForMember() throws Exception {
        when(coreSecurityService.isActiveOrganizationMember(10L)).thenReturn(true);
        when(userService.getOrganizationForMember(42L, 10L))
                .thenReturn(OrganizationDto.builder().id(10L).name("FF Muster").build());

        mockMvc.perform(
                        get("/api/v1/self/organizations/10")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 42L)))
                .andExpect(status().isOk());
    }
}
