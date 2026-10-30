/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.trainingcourse.service.TrainingCourseCatalogService;
import com.emberone.trainingcourse.service.TrainingCourseRecordService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserTrainingCourseSelfController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class UserTrainingCourseSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean TrainingCourseRecordService recordService;
    @MockitoBean TrainingCourseCatalogService catalogService;

    @Test
    void selfAvailableTypesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/training-courses/available-types"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void selfAvailableTypesAllowedForTenantUser() throws Exception {
        when(catalogService.listAvailableTypesForUser(SecurityTestFixtures.USER_ID))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/self/training-courses/available-types")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
