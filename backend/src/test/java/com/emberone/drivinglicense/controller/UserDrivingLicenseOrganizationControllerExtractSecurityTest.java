/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.drivinglicense.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.drivinglicense.service.DrivingLicenseCatalogService;
import com.emberone.drivinglicense.service.DrivingLicenseRecordService;
import com.emberone.platform.ai.extraction.DocumentExtractionResult;
import com.emberone.platform.ai.extraction.DocumentExtractionService;
import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.user.service.UserService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserDrivingLicenseOrganizationController.class)
@Import(SecurityWebMvcTestConfig.class)
class UserDrivingLicenseOrganizationControllerExtractSecurityTest {

    private static final String EXTRACT =
            "/api/v1/auth/organizations/10/users/5/driving-license/extract";

    @Autowired MockMvc mockMvc;

    @MockitoBean DrivingLicenseRecordService drivingLicenseRecordService;
    @MockitoBean DrivingLicenseCatalogService drivingLicenseCatalogService;
    @MockitoBean UserService userService;
    @MockitoBean DocumentExtractionService documentExtractionService;

    @MockitoBean(name = "securityService")
    SecurityService securityService;

    private static MockMultipartFile proof() {
        return new MockMultipartFile("file", "proof.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }

    @Test
    void extractRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart(EXTRACT).file(proof())).andExpect(status().isUnauthorized());
    }

    @Test
    void extractForbiddenWithoutWritePermission() throws Exception {
        when(securityService.hasDrivingLicenseWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(multipart(EXTRACT).file(proof()).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());

        verify(documentExtractionService, never())
                .extractForUser(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void extractAllowedWithWritePermission() throws Exception {
        when(securityService.hasDrivingLicenseWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(documentExtractionService.extractForUser(
                        eq("DRIVING_LICENSE_RECORD"), any(), isNull(), eq(5L)))
                .thenReturn(new DocumentExtractionResult(Map.of(), List.of()));

        mockMvc.perform(multipart(EXTRACT).file(proof()).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());

        verify(userService, atLeastOnce())
                .assertUserBelongsToOrganization(5L, SecurityTestFixtures.ORG_ID);
    }
}
