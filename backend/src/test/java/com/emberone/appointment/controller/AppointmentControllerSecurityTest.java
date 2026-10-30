/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.appointment.dto.AppointmentDto;
import com.emberone.appointment.list.AppointmentOrganizationListFilterOptions;
import com.emberone.appointment.service.AppointmentResponseService;
import com.emberone.appointment.service.AppointmentService;
import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AppointmentController.class)
@Import(SecurityWebMvcTestConfig.class)
class AppointmentControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AppointmentService appointmentService;
    @MockitoBean AppointmentResponseService appointmentResponseService;
    @MockitoBean AppointmentOrganizationListFilterOptions organizationListFilterOptions;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void appointmentDetailRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/organizations/10/appointments/5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void appointmentDetailForbiddenWithoutPermission() throws Exception {
        when(coreSecurityService.hasAppointmentReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/appointments/5")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void appointmentDetailAllowedWithPermission() throws Exception {
        when(coreSecurityService.hasAppointmentReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(appointmentService.getAppointment(
                        SecurityTestFixtures.ORG_ID, SecurityTestFixtures.APPOINTMENT_ID))
                .thenReturn(
                        AppointmentDto.builder().id(SecurityTestFixtures.APPOINTMENT_ID).build());

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/appointments/5")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
