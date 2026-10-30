/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.profileimage.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.SecurityService;
import com.emberone.platform.security.TokenService;
import com.emberone.profileimage.service.ProfileImageService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SelfImageController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class SelfImageControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ProfileImageService profileImageService;

    @MockitoBean(name = "securityService")
    SecurityService securityService;

    @Test
    void selfProfileImageDownloadRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/profile-image/download"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void selfProfileImageDownloadAllowedForTenantUser() throws Exception {
        when(securityService.isSelfServiceUser()).thenReturn(true);
        when(profileImageService.downloadProfileImage(SecurityTestFixtures.USER_ID))
                .thenReturn(
                        new ProfileImageService.ProfileImageDownload(
                                new InputStreamResource(new ByteArrayInputStream(new byte[0])),
                                MediaType.IMAGE_JPEG,
                                "\"etag\""));

        mockMvc.perform(
                        get("/api/v1/self/profile-image/download")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
