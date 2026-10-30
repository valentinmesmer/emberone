/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.management.exception.ManagementActivationNotFoundException;
import com.emberone.management.service.ManagementAccessService;
import com.emberone.management.service.ManagementActivationService;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.SecurityService;
import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.JwtTestSupport;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ManagementSelfController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class ManagementSelfControllerSecurityTest {

    private static final String ACTIVATIONS = "/api/v1/self/management/activations";
    private static final String ORGANIZATIONS = "/api/v1/self/management/organizations";

    @Autowired MockMvc mockMvc;

    @MockitoBean ManagementActivationService activationService;

    @MockitoBean ManagementAccessService accessService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(ORGANIZATIONS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ACTIVATIONS)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ACTIVATIONS)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(ACTIVATIONS + "/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUserTokenWithoutTenant() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(true);

        mockMvc.perform(
                        get(ORGANIZATIONS)
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, null, 42L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listsOpenTargetsForTenantUser() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(true);
        when(activationService.listTargets(7L, 42L)).thenReturn(List.of());

        mockMvc.perform(
                        get(ORGANIZATIONS)
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 42L)))
                .andExpect(status().isOk());
    }

    @Test
    void targetWithoutLiveRightsIsNotFound() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(true);
        when(accessService.rightsIn(9L)).thenReturn(Set.of());

        mockMvc.perform(
                        get(ORGANIZATIONS + "/9")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 42L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void targetWithLiveRightsReturnsMasterData() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(true);
        when(accessService.rightsIn(9L)).thenReturn(Set.of(PermissionType.USER_READ));

        mockMvc.perform(
                        get(ORGANIZATIONS + "/9")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 42L)))
                .andExpect(status().isOk());
    }

    @Test
    void activateRejectsMissingFields() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(true);

        mockMvc.perform(
                        post(ACTIVATIONS)
                                .contentType("application/json")
                                .content("{}")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 42L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void endOfForeignOrUnknownActivationIsNotFound() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(true);
        doThrow(new ManagementActivationNotFoundException(5L))
                .when(activationService)
                .endOwn(any(Long.class), any(Long.class), any(Long.class));

        mockMvc.perform(
                        delete(ACTIVATIONS + "/5")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 42L)))
                .andExpect(status().isNotFound());
    }
}
