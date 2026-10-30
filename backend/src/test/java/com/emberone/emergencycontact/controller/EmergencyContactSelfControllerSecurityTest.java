/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.emergencycontact.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.emergencycontact.service.EmergencyContactService;
import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmergencyContactSelfController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class EmergencyContactSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean EmergencyContactService emergencyContactService;

    @Test
    void emergencyContactsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/emergency-contacts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emergencyContactsAllowedForTenantUser() throws Exception {
        when(emergencyContactService.getEmergencyContactsForUser(SecurityTestFixtures.USER_ID))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/self/emergency-contacts")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
