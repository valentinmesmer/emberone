/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.courseoffering.dto.CourseRegistrationDto;
import com.emberone.courseoffering.dto.CourseRegistrationPageDto;
import com.emberone.courseoffering.dto.CourseRegistrationSummaryDto;
import com.emberone.courseoffering.service.CourseRegistrationQueryService;
import com.emberone.courseoffering.service.CourseRegistrationService;
import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CourseRegistrationController.class)
@Import(SecurityWebMvcTestConfig.class)
class CourseRegistrationControllerSecurityTest {

    private static final String BASE =
            "/api/v1/auth/organizations/10/course-offerings/5/registrations";

    @Autowired MockMvc mockMvc;

    @MockitoBean CourseRegistrationService registrationService;
    @MockitoBean CourseRegistrationQueryService queryService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    void listForbiddenWithoutCourseOfferingRead() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(get(BASE).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAllowedWithCourseOfferingRead() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(queryService.listForOffering(eq(SecurityTestFixtures.ORG_ID), eq(5L), any(), any()))
                .thenReturn(CourseRegistrationPageDto.builder().content(List.of()).build());

        mockMvc.perform(get(BASE).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void getForbiddenWithoutCourseOfferingRead() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(get(BASE + "/9").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllowedWithCourseOfferingRead() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(queryService.getForOffering(SecurityTestFixtures.ORG_ID, 5L, 9L))
                .thenReturn(CourseRegistrationDto.builder().id(9L).build());

        mockMvc.perform(get(BASE + "/9").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void summaryForbiddenWithoutCourseOfferingRead() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(get(BASE + "/summary").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void summaryAllowedWithCourseOfferingRead() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(queryService.summary(SecurityTestFixtures.ORG_ID, 5L))
                .thenReturn(CourseRegistrationSummaryDto.builder().maxParticipants(5).build());

        mockMvc.perform(get(BASE + "/summary").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void promoteForbiddenWithReadOnly() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(post(BASE + "/9/promote").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void promoteAllowedWithWrite() throws Exception {
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(registrationService.promote(SecurityTestFixtures.ORG_ID, 5L, 9L))
                .thenReturn(CourseRegistrationDto.builder().id(9L).build());

        mockMvc.perform(post(BASE + "/9/promote").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void removeForbiddenWithoutWrite() throws Exception {
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        post(BASE + "/9/remove")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"Kein Bedarf\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeAllowedWithWriteAndPassesReason() throws Exception {
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(registrationService.remove(SecurityTestFixtures.ORG_ID, 5L, 9L, "Kein Bedarf"))
                .thenReturn(CourseRegistrationDto.builder().id(9L).build());

        mockMvc.perform(
                        post(BASE + "/9/remove")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"Kein Bedarf\"}"))
                .andExpect(status().isOk());

        verify(registrationService).remove(SecurityTestFixtures.ORG_ID, 5L, 9L, "Kein Bedarf");
    }

    @Test
    void removeRejectsMissingReason() throws Exception {
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);

        mockMvc.perform(
                        post(BASE + "/9/remove")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }
}
