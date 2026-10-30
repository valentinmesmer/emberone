/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.appointment.service.AppointmentResponseService;
import com.emberone.appointment.service.AppointmentService;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.JwtTestSupport;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizationAppointmentSelfController.class)
@Import(SecurityWebMvcTestConfig.class)
class OrganizationAppointmentSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AppointmentService appointmentService;

    @MockitoBean AppointmentResponseService appointmentResponseService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void markReadRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/self/organizations/10/appointments/5/mark-read"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void markReadForbiddenWhenNotOrganizationMember() throws Exception {
        when(coreSecurityService.isActiveOrganizationMember(10L)).thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/self/organizations/10/appointments/5/mark-read")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 42L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void markReadAllowedWhenOrganizationMember() throws Exception {
        when(coreSecurityService.isActiveOrganizationMember(10L)).thenReturn(true);
        doNothing().when(appointmentService).markRead(10L, 5L);

        mockMvc.perform(
                        post("/api/v1/self/organizations/10/appointments/5/mark-read")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 42L)))
                .andExpect(status().isNoContent());
    }

    @Test
    void markAllReadAllowedWhenOrganizationMember() throws Exception {
        when(coreSecurityService.isActiveOrganizationMember(10L)).thenReturn(true);
        doNothing().when(appointmentService).markAllReadForOrganization(10L);

        mockMvc.perform(
                        post("/api/v1/self/organizations/10/appointments/mark-all-read")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 42L)))
                .andExpect(status().isNoContent());
    }

    @Test
    void submitResponseForbiddenWhenNotOrganizationMember() throws Exception {
        when(coreSecurityService.isActiveOrganizationMember(10L)).thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/self/organizations/10/appointments/5/responses")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 42L))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"value\":\"YES\"}"))
                .andExpect(status().isForbidden());
    }
}
