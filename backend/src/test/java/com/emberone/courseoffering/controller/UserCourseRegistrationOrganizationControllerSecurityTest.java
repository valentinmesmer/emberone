/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.courseoffering.dto.UserCourseRegistrationPageDto;
import com.emberone.courseoffering.service.CourseRegistrationQueryService;
import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The member view is guarded by the organization's training-course read permission; the
 * course-offering permissions must neither grant nor be required.
 */
@WebMvcTest(UserCourseRegistrationOrganizationController.class)
@Import(SecurityWebMvcTestConfig.class)
class UserCourseRegistrationOrganizationControllerSecurityTest {

    private static final String URL = "/api/v1/auth/organizations/10/users/3/course-registrations";

    @Autowired MockMvc mockMvc;

    @MockitoBean CourseRegistrationQueryService queryService;
    @MockitoBean UserService userService;

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
        when(queryService.listForUser(eq(3L), any(), any()))
                .thenReturn(UserCourseRegistrationPageDto.builder().content(List.of()).build());

        mockMvc.perform(get(URL).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());

        // Checked by the path interceptor and again by the controller itself.
        verify(userService, atLeastOnce())
                .assertUserBelongsToOrganization(3L, SecurityTestFixtures.ORG_ID);
    }
}
