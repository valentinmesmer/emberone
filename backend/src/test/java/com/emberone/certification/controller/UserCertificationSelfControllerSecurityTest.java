/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.certification.dto.UserCertificationStatusDto;
import com.emberone.certification.service.CertificationAssignmentService;
import com.emberone.certification.service.CertificationRecordService;
import com.emberone.certification.service.CertificationStatusService;
import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserCertificationSelfController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class UserCertificationSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean CertificationStatusService statusService;
    @MockitoBean CertificationRecordService recordService;
    @MockitoBean CertificationAssignmentService assignmentService;

    @Test
    void selfCertificationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/certifications/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void selfCertificationStatusAllowedForTenantUser() throws Exception {
        when(statusService.getStatusForUser(SecurityTestFixtures.USER_ID))
                .thenReturn(
                        UserCertificationStatusDto.builder()
                                .userId(SecurityTestFixtures.USER_ID)
                                .items(List.of())
                                .build());

        mockMvc.perform(
                        get("/api/v1/self/certifications/status")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
