/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.certification.dto.CertificationOverviewCountsDto;
import com.emberone.certification.dto.CertificationOverviewRowDto;
import com.emberone.certification.list.CertificationOrganizationListFilterOptions;
import com.emberone.certification.list.CertificationOverviewListFilter;
import com.emberone.certification.service.CertificationStatusService;
import com.emberone.platform.security.SecurityService;
import com.emberone.platform.web.PageDto;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizationCertificationController.class)
@Import(SecurityWebMvcTestConfig.class)
class OrganizationCertificationControllerSecurityTest {

    private static final String OVERVIEW =
            "/api/v1/auth/organizations/10/certifications/status-overview";
    private static final String COUNTS = OVERVIEW + "/counts";

    @Autowired MockMvc mockMvc;

    @MockitoBean CertificationStatusService statusService;

    @MockitoBean CertificationOrganizationListFilterOptions organizationListFilterOptions;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void overviewAndCountsRequireAuthentication() throws Exception {
        mockMvc.perform(get(OVERVIEW)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(COUNTS)).andExpect(status().isUnauthorized());
    }

    @Test
    void overviewAndCountsForbiddenWithoutPermission() throws Exception {
        when(coreSecurityService.hasCertificationReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(get(OVERVIEW).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(COUNTS).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
        verify(statusService, never())
                .getStatusOverview(any(), any(), any(CertificationOverviewListFilter.class));
        verify(statusService, never()).getStatusOverviewCounts(any());
    }

    @Test
    void overviewReturnsPageForPermittedUser() throws Exception {
        when(coreSecurityService.hasCertificationReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(statusService.getStatusOverview(eq(SecurityTestFixtures.ORG_ID), any(), any()))
                .thenReturn(
                        PageDto.<CertificationOverviewRowDto>builder()
                                .content(List.of())
                                .totalElements(0)
                                .build());

        mockMvc.perform(
                        get(OVERVIEW)
                                .param("status", "EXPIRED")
                                .param("search", "ada")
                                .param("size", "20")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
        ArgumentCaptor<ListQuery> query = ArgumentCaptor.forClass(ListQuery.class);
        verify(statusService)
                .getStatusOverview(
                        eq(SecurityTestFixtures.ORG_ID),
                        query.capture(),
                        eq(CertificationOverviewListFilter.parse(List.of("EXPIRED"), null)));
        assertThat(query.getValue()).isEqualTo(new ListQuery(null, 20, "ada", null));
    }

    @Test
    void invalidStatusIsBadRequest() throws Exception {
        when(coreSecurityService.hasCertificationReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);

        mockMvc.perform(
                        get(OVERVIEW)
                                .param("status", "BOGUS")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void countsReturnExactFieldNames() throws Exception {
        when(coreSecurityService.hasCertificationReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(statusService.getStatusOverviewCounts(SecurityTestFixtures.ORG_ID))
                .thenReturn(
                        CertificationOverviewCountsDto.builder()
                                .all(10)
                                .missing(1)
                                .expired(2)
                                .expiringSoon(3)
                                .valid(4)
                                .build());

        mockMvc.perform(get(COUNTS).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.all").value(10))
                .andExpect(jsonPath("$.missing").value(1))
                .andExpect(jsonPath("$.expired").value(2))
                .andExpect(jsonPath("$.expiringSoon").value(3))
                .andExpect(jsonPath("$.valid").value(4));
    }
}
