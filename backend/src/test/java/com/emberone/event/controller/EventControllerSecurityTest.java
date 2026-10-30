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
import com.emberone.event.dto.EventAttendancePageDto;
import com.emberone.event.list.EventOrganizationListFilterOptions;
import com.emberone.event.service.EventAttendanceService;
import com.emberone.event.service.EventService;
import com.emberone.platform.security.SecurityService;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class EventControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean EventService eventService;
    @MockitoBean EventAttendanceService eventAttendanceService;
    @MockitoBean EventOrganizationListFilterOptions organizationListFilterOptions;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void attendancesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/organizations/10/events/service/5/attendances"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void attendancesForbiddenWithoutPermission() throws Exception {
        when(coreSecurityService.hasEventAttendanceReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/events/service/5/attendances")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOperationEventForbiddenWithoutPermission() throws Exception {
        when(coreSecurityService.hasOperationEventReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/events/operation/5")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOperationEventAllowedWithPermission() throws Exception {
        when(coreSecurityService.hasOperationEventReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(eventService.getOperationEvent(SecurityTestFixtures.ORG_ID, 5L))
                .thenReturn(com.emberone.event.dto.OperationEventDto.builder().id(5L).build());

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/events/operation/5")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void attendancesAllowedWithPermission() throws Exception {
        when(coreSecurityService.hasEventAttendanceReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(eventAttendanceService.listAttendances(
                        SecurityTestFixtures.ORG_ID,
                        EventType.SERVICE,
                        5L,
                        new ListQuery(0, null, null, null)))
                .thenReturn(EventAttendancePageDto.builder().content(List.of()).build());

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/events/service/5/attendances")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
