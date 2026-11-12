/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.qualification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.SecurityService;
import com.emberone.qualification.dto.UserQualificationDetailDto;
import com.emberone.qualification.service.QualificationDetailService;
import com.emberone.qualification.service.QualificationDetailService.EvidenceAccess;
import com.emberone.qualification.service.QualificationOrgDecisionService;
import com.emberone.qualification.service.QualificationStatusService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserQualificationOrganizationController.class)
@Import(SecurityWebMvcTestConfig.class)
class UserQualificationOrganizationControllerSecurityTest {

    private static final String URL = "/api/v1/auth/organizations/10/users/20/qualifications/5";

    @Autowired MockMvc mockMvc;

    @MockitoBean QualificationStatusService statusService;
    @MockitoBean QualificationOrgDecisionService orgDecisionService;
    @MockitoBean QualificationDetailService detailService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void detailRequiresAuthentication() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void detailForbiddenWithoutQualificationRead() throws Exception {
        when(coreSecurityService.hasQualificationReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);
        when(coreSecurityService.hasCertificationReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);

        mockMvc.perform(get(URL).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());

        verify(detailService, never()).getOrgDetail(any(), any(), any(), any());
    }

    @Test
    void detailPassesOnlyGrantedEvidenceAccess() throws Exception {
        when(coreSecurityService.hasQualificationReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(coreSecurityService.hasCertificationReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(detailService.getOrgDetail(any(), any(), any(), any()))
                .thenReturn(UserQualificationDetailDto.builder().build());

        mockMvc.perform(get(URL).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());

        verify(detailService)
                .getOrgDetail(
                        eq(SecurityTestFixtures.ORG_ID),
                        eq(20L),
                        eq(5L),
                        eq(new EvidenceAccess(true, false, false)));
    }
}
