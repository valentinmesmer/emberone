/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.SecurityService;
import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AiSelfController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class AiSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AiCapabilitiesService capabilitiesService;

    @MockitoBean(name = "securityService")
    SecurityService securityService;

    @Test
    void capabilitiesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/ai/capabilities")).andExpect(status().isUnauthorized());
    }

    @Test
    void capabilitiesForbiddenForNonSelfServiceUser() throws Exception {
        when(securityService.isSelfServiceUser()).thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/self/ai/capabilities")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void capabilitiesAllowedForSelfServiceUser() throws Exception {
        when(securityService.isSelfServiceUser()).thenReturn(true);
        when(capabilitiesService.capabilitiesForCurrentUser())
                .thenReturn(
                        new AiCapabilitiesDto(
                                false,
                                new AiCapabilitiesDto.FunctionCapability(false),
                                new AiCapabilitiesDto.DocumentExtraction(false, Map.of())));

        mockMvc.perform(
                        get("/api/v1/self/ai/capabilities")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.helpChat.available").value(false))
                .andExpect(jsonPath("$.documentExtraction.available").value(false));
    }
}
