/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.trainingcourse.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.ai.extraction.DocumentExtractionService;
import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.trainingcourse.dto.TrainingCourseUserRecordDto;
import com.emberone.trainingcourse.service.TrainingCourseCatalogService;
import com.emberone.trainingcourse.service.TrainingCourseRecordService;
import com.emberone.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserTrainingCourseOrganizationController.class)
@Import(SecurityWebMvcTestConfig.class)
class UserTrainingCourseOrganizationControllerSecurityTest {

    private static final String RECORD =
            "/api/v1/auth/organizations/10/users/42/training-courses/records/7";

    @Autowired MockMvc mockMvc;

    @MockitoBean TrainingCourseRecordService recordService;
    @MockitoBean TrainingCourseCatalogService catalogService;
    @MockitoBean UserService userService;
    @MockitoBean DocumentExtractionService documentExtractionService;

    @MockitoBean(name = "securityService")
    SecurityService securityService;

    @Test
    void recordRequiresAuthentication() throws Exception {
        mockMvc.perform(get(RECORD)).andExpect(status().isUnauthorized());
    }

    @Test
    void recordForbiddenWithoutReadPermission() throws Exception {
        when(securityService.hasTrainingCourseReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(get(RECORD).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void recordAllowedWithReadPermission() throws Exception {
        when(securityService.hasTrainingCourseReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(recordService.getRecord(SecurityTestFixtures.USER_ID, 7L))
                .thenReturn(TrainingCourseUserRecordDto.builder().id(7L).build());

        mockMvc.perform(get(RECORD).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
