/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.appointment.dto.UpcomingAppointmentsDto;
import com.emberone.appointment.service.AppointmentResponseService;
import com.emberone.appointment.service.AppointmentService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SelfAppointmentController.class)
@Import(SecurityWebMvcTestConfig.class)
class SelfAppointmentControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AppointmentService appointmentService;
    @MockitoBean AppointmentResponseService appointmentResponseService;

    @Test
    void upcomingAppointmentsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/appointments/upcoming"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upcomingAppointmentsAllowedForTenantUser() throws Exception {
        when(appointmentService.listUpcomingAcrossOrganizations(5))
                .thenReturn(
                        UpcomingAppointmentsDto.builder().content(List.of()).totalCount(0).build());

        mockMvc.perform(
                        get("/api/v1/self/appointments/upcoming")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void unreadCountsAllowedForTenantUser() throws Exception {
        when(appointmentService.countUnreadAcrossOrganizations()).thenReturn(Map.of(10L, 1L));

        mockMvc.perform(
                        get("/api/v1/self/appointments/my-unread-counts")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
