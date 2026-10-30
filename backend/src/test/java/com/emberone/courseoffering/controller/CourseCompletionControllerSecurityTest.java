/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.courseoffering.dto.CourseCompletionDto;
import com.emberone.courseoffering.service.CourseCompletionService;
import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CourseCompletionController.class)
@Import(SecurityWebMvcTestConfig.class)
class CourseCompletionControllerSecurityTest {

    private static final String BASE = "/api/v1/auth/organizations/10/course-offerings/5";

    @Autowired MockMvc mockMvc;

    @MockitoBean CourseCompletionService completionService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void allEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get(BASE + "/completion")).andExpect(status().isUnauthorized());
        mockMvc.perform(
                        put(BASE + "/registrations/9/final-result")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"passed\":true}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/complete")).andExpect(status().isUnauthorized());
    }

    @Test
    void getForbiddenWithoutRead() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(get(BASE + "/completion").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(completionService);
    }

    @Test
    void getAllowedWithRead() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(completionService.get(SecurityTestFixtures.ORG_ID, 5L))
                .thenReturn(CourseCompletionDto.builder().build());

        mockMvc.perform(get(BASE + "/completion").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void writeEndpointsForbiddenWithReadOnly() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        put(BASE + "/registrations/9/final-result")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"passed\":true}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/complete").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(completionService);
    }

    @Test
    void finalResultAllowedWithWritePassesResult() throws Exception {
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(completionService.setFinalResult(SecurityTestFixtures.ORG_ID, 5L, 9L, false))
                .thenReturn(CourseCompletionDto.builder().build());

        mockMvc.perform(
                        put(BASE + "/registrations/9/final-result")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"passed\":false}"))
                .andExpect(status().isOk());
        verify(completionService).setFinalResult(SecurityTestFixtures.ORG_ID, 5L, 9L, false);
    }

    @Test
    void completeAllowedWithWrite() throws Exception {
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(completionService.complete(SecurityTestFixtures.ORG_ID, 5L))
                .thenReturn(CourseCompletionDto.builder().build());

        mockMvc.perform(post(BASE + "/complete").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
        verify(completionService).complete(SecurityTestFixtures.ORG_ID, 5L);
    }
}
