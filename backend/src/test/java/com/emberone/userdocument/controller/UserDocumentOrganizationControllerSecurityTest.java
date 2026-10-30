/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.SecurityService;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.userdocument.list.UserDocumentListFilterOptions;
import com.emberone.userdocument.service.UserDocumentService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserDocumentOrganizationController.class)
@Import(SecurityWebMvcTestConfig.class)
class UserDocumentOrganizationControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserDocumentService userDocumentService;

    @MockitoBean UserDocumentListFilterOptions userDocumentListFilterOptions;

    @MockitoBean(name = "securityService")
    SecurityService documentSecurityService;

    @Test
    void listFilterOptionsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/organizations/10/users/42/documents/list-filter-options"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listFilterOptionsForbiddenWithoutPermission() throws Exception {
        when(documentSecurityService.hasDocumentReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/users/42/documents/list-filter-options")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listFilterOptionsAllowedWithPermission() throws Exception {
        when(documentSecurityService.hasDocumentReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        doNothing()
                .when(userDocumentService)
                .assertUserBelongsToOrganization(
                        SecurityTestFixtures.USER_ID, SecurityTestFixtures.ORG_ID);
        when(userDocumentListFilterOptions.forUser(SecurityTestFixtures.USER_ID))
                .thenReturn(new ListFilterOptionsDto(Map.of()));

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/users/42/documents/list-filter-options")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void documentsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/organizations/10/users/42/documents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void documentsForbiddenWithoutPermission() throws Exception {
        when(documentSecurityService.hasDocumentReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/users/42/documents")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void documentsAllowedWithPermission() throws Exception {
        when(documentSecurityService.hasDocumentReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        doNothing()
                .when(userDocumentService)
                .assertUserBelongsToOrganization(
                        SecurityTestFixtures.USER_ID, SecurityTestFixtures.ORG_ID);
        when(userDocumentService.listDocuments(
                        org.mockito.ArgumentMatchers.eq(SecurityTestFixtures.USER_ID),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(
                        com.emberone.userdocument.dto.UserDocumentPageDto.builder()
                                .content(List.of())
                                .build());

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/users/42/documents")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
