/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.event.domain.EventType;
import com.emberone.event.dto.EventAttendanceSelfItemDto;
import com.emberone.event.service.EventAttendanceService;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.JwtTestSupport;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventSelfController.class)
@Import(SecurityWebMvcTestConfig.class)
class EventSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean EventAttendanceService eventAttendanceService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void myAttendanceRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        get("/api/v1/self/organizations/10/events/service/attendances/me")
                                .param("eventIds", "5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void myAttendanceForbiddenWhenNotOrganizationMember() throws Exception {
        when(coreSecurityService.isActiveOrganizationMember(10L)).thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/self/organizations/10/events/service/attendances/me")
                                .param("eventIds", "5")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 42L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void myAttendancesBatchAllowedWhenOrganizationMember() throws Exception {
        when(coreSecurityService.isActiveOrganizationMember(10L)).thenReturn(true);
        when(eventAttendanceService.getAttendancesForUser(10L, EventType.SERVICE, List.of(5L, 6L)))
                .thenReturn(
                        List.of(
                                EventAttendanceSelfItemDto.builder()
                                        .eventId(5L)
                                        .attending(true)
                                        .build()));

        mockMvc.perform(
                        get("/api/v1/self/organizations/10/events/service/attendances/me")
                                .param("eventIds", "5", "6")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 42L)))
                .andExpect(status().isOk());
    }
}
