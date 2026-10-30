/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.onboardingdocument.service.SelfEntryDocumentProofExportService;
import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SelfEntryDocumentFulfillmentController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class SelfEntryDocumentFulfillmentControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean SelfEntryDocumentProofExportService selfEntryDocumentProofExportService;

    @Test
    void proofExportRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/entry-documents/proof-export"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void proofExportAllowedForTenantUser() throws Exception {
        when(selfEntryDocumentProofExportService.exportProofPdf(SecurityTestFixtures.USER_ID))
                .thenReturn(new byte[] {'%', 'P', 'D', 'F'});

        mockMvc.perform(
                        get("/api/v1/self/entry-documents/proof-export")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
