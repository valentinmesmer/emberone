/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.onboardingdocument.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.onboardingdocument.dto.CreateOrganizationOnboardingDocumentDefinitionRequest;
import com.emberone.onboardingdocument.service.OrganizationOnboardingDocumentDefinitionService;
import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AdminOrganizationOnboardingDocumentController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminOrganizationOnboardingDocumentControllerSecurityTest {

    private static final long TENANT_ID = 7L;
    private static final long ORG_ID = 10L;

    @Autowired MockMvc mockMvc;

    @Autowired ObjectMapper objectMapper;

    @MockitoBean OrganizationOnboardingDocumentDefinitionService definitionService;

    @MockitoBean(name = "securityService")
    SecurityService securityService;

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        get(
                                "/api/v1/auth/admin/tenants/"
                                        + TENANT_ID
                                        + "/organizations/"
                                        + ORG_ID
                                        + "/onboarding-documents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listForbiddenWithoutTenantAccess() throws Exception {
        when(securityService.canAccessTenant(TENANT_ID)).thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/"
                                        + TENANT_ID
                                        + "/organizations/"
                                        + ORG_ID
                                        + "/onboarding-documents")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAllowedWithTenantAccess() throws Exception {
        when(securityService.canAccessTenant(TENANT_ID)).thenReturn(true);
        when(definitionService.list(TENANT_ID, ORG_ID)).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/"
                                        + TENANT_ID
                                        + "/organizations/"
                                        + ORG_ID
                                        + "/onboarding-documents")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void createForbiddenWithoutTenantAccess() throws Exception {
        when(securityService.canAccessTenant(TENANT_ID)).thenReturn(false);

        CreateOrganizationOnboardingDocumentDefinitionRequest request =
                new CreateOrganizationOnboardingDocumentDefinitionRequest();
        request.setAbbreviation("ORG");
        request.setName("Org Doc");
        request.setFulfillmentMode(
                com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode.CONFIRMED);

        mockMvc.perform(
                        post("/api/v1/auth/admin/tenants/"
                                        + TENANT_ID
                                        + "/organizations/"
                                        + ORG_ID
                                        + "/onboarding-documents")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
