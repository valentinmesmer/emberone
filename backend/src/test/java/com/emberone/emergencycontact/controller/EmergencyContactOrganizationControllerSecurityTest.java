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
import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmergencyContactOrganizationController.class)
@Import(SecurityWebMvcTestConfig.class)
class EmergencyContactOrganizationControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean EmergencyContactService emergencyContactService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void emergencyContactsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/organizations/10/emergency-contacts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emergencyContactsForbiddenWithoutPermission() throws Exception {
        when(coreSecurityService.hasEmergencyContactReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/emergency-contacts")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void emergencyContactsAllowedWithPermission() throws Exception {
        when(coreSecurityService.hasEmergencyContactReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(emergencyContactService.getEmergencyContactsForOrganization(
                        SecurityTestFixtures.ORG_ID))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/emergency-contacts")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
