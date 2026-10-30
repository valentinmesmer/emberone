/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.courseoffering.dto.OrganizationCourseRegistrationPageDto;
import com.emberone.courseoffering.service.CourseRegistrationQueryService;
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

/**
 * The organization-wide registration list is guarded by the training-course read permission; the
 * course-offering permissions must neither grant nor be required.
 */
@WebMvcTest(OrganizationCourseRegistrationController.class)
@Import(SecurityWebMvcTestConfig.class)
class OrganizationCourseRegistrationControllerSecurityTest {

    private static final String URL = "/api/v1/auth/organizations/10/course-registrations";

    @Autowired MockMvc mockMvc;

    @MockitoBean CourseRegistrationQueryService queryService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void forbiddenWithoutTrainingCourseReadEvenWithCourseOfferingRead() throws Exception {
        when(coreSecurityService.hasTrainingCourseReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);

        mockMvc.perform(get(URL).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowedWithTrainingCourseReadWithoutCourseOfferingRead() throws Exception {
        when(coreSecurityService.hasTrainingCourseReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);
        when(queryService.listForOrganization(eq(SecurityTestFixtures.ORG_ID), any(), any()))
                .thenReturn(
                        OrganizationCourseRegistrationPageDto.builder().content(List.of()).build());

        mockMvc.perform(get(URL).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
