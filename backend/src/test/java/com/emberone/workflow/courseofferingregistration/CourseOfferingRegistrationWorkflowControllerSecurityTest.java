/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.courseofferingregistration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.SecurityService;
import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.WorkflowDto;
import com.emberone.workflow.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CourseOfferingRegistrationWorkflowController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class CourseOfferingRegistrationWorkflowControllerSecurityTest {

    private static final String URL = "/api/v1/auth/workflows/course-offering-registration";
    private static final String PAYLOAD = "{\"offeringId\":5}";

    @Autowired MockMvc mockMvc;

    @MockitoBean WorkflowService workflowService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void openRequiresAuthentication() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openForbiddenForNonSelfServiceUser() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(false);

        mockMvc.perform(
                        post(URL)
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(PAYLOAD))
                .andExpect(status().isForbidden());
    }

    @Test
    void openAllowedForSelfServiceUserAndUsesOfferingAsSubject() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(true);
        when(workflowService.open(
                        eq(WorkflowType.COURSE_OFFERING_REGISTRATION.getId()),
                        any(),
                        eq("COURSE_OFFERING"),
                        eq(5L)))
                .thenReturn(WorkflowDto.builder().id(1L).build());

        mockMvc.perform(
                        post(URL)
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(PAYLOAD))
                .andExpect(status().isOk());

        verify(workflowService)
                .open(
                        eq(WorkflowType.COURSE_OFFERING_REGISTRATION.getId()),
                        any(),
                        eq("COURSE_OFFERING"),
                        eq(5L));
    }

    @Test
    void openRejectsMissingOffering() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(true);

        mockMvc.perform(
                        post(URL)
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
