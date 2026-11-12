/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.qualification.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.TokenService;
import com.emberone.qualification.dto.SelfQualificationDetailDto;
import com.emberone.qualification.service.QualificationDetailService;
import com.emberone.qualification.service.QualificationStatusService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserQualificationSelfController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class UserQualificationSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean QualificationStatusService statusService;
    @MockitoBean QualificationDetailService detailService;

    @Test
    void selfDetailRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/qualifications/5")).andExpect(status().isUnauthorized());
    }

    @Test
    void selfDetailAllowedForTenantUserAndScopedToCaller() throws Exception {
        when(detailService.getSelfDetail(SecurityTestFixtures.USER_ID, 5L))
                .thenReturn(SelfQualificationDetailDto.builder().build());

        mockMvc.perform(
                        get("/api/v1/self/qualifications/5")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
