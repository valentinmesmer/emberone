/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.appointment.service.AppointmentDocumentService;
import com.emberone.platform.security.SecurityService;
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

@WebMvcTest(AppointmentDocumentController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class AppointmentDocumentControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AppointmentDocumentService appointmentDocumentService;

    @MockitoBean(name = "securityService")
    SecurityService documentSecurityService;

    @Test
    void attachmentsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/organizations/10/appointments/5/attachments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void attachmentsForbiddenWithoutPermission() throws Exception {
        when(documentSecurityService.hasAppointmentReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/appointments/5/attachments")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void attachmentsAllowedWithPermission() throws Exception {
        when(documentSecurityService.hasAppointmentReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(appointmentDocumentService.listAttachments(
                        SecurityTestFixtures.ORG_ID, SecurityTestFixtures.APPOINTMENT_ID))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/appointments/5/attachments")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
