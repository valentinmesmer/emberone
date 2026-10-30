/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.userdocument.dto.UserDocumentPageDto;
import com.emberone.userdocument.list.UserDocumentSelfListFilterOptions;
import com.emberone.userdocument.service.UserDocumentService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserDocumentSelfController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class UserDocumentSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserDocumentService userDocumentService;
    @MockitoBean UserDocumentSelfListFilterOptions selfListFilterOptions;

    @Test
    void listFilterOptionsSameAccessAsListDocuments() throws Exception {
        mockMvc.perform(get("/api/v1/self/documents/list-filter-options"))
                .andExpect(status().isUnauthorized());

        when(selfListFilterOptions.filterOptions()).thenReturn(new ListFilterOptionsDto(Map.of()));
        mockMvc.perform(
                        get("/api/v1/self/documents/list-filter-options")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void selfDocumentsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/documents")).andExpect(status().isUnauthorized());
    }

    @Test
    void selfDocumentsAllowedForTenantUser() throws Exception {
        when(userDocumentService.listDocuments(
                        org.mockito.ArgumentMatchers.eq(SecurityTestFixtures.USER_ID),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(UserDocumentPageDto.builder().content(List.of()).build());

        mockMvc.perform(get("/api/v1/self/documents").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void markAllViewedAllowedForTenantUser() throws Exception {
        mockMvc.perform(
                        post("/api/v1/self/documents/mark-all-viewed")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isNoContent());

        verify(userDocumentService).markAllViewed(SecurityTestFixtures.USER_ID);
    }
}
