/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.profileimage.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.SecurityService;
import com.emberone.profileimage.service.ProfileImageService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.user.service.UserService;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProfileImageController.class)
@Import(SecurityWebMvcTestConfig.class)
class ProfileImageControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ProfileImageService profileImageService;
    @MockitoBean UserService userService;

    @MockitoBean(name = "securityService")
    SecurityService securityService;

    @Test
    void profileImageDownloadRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/organizations/10/users/42/profile-image/download"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profileImageDownloadForbiddenWhenDenied() throws Exception {
        when(securityService.hasProfileImageReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/users/42/profile-image/download")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void profileImageDownloadAllowedWhenPermitted() throws Exception {
        when(securityService.hasProfileImageReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        doNothing()
                .when(userService)
                .assertUserBelongsToOrganization(
                        SecurityTestFixtures.USER_ID, SecurityTestFixtures.ORG_ID);
        when(profileImageService.downloadProfileImage(SecurityTestFixtures.USER_ID))
                .thenReturn(
                        new ProfileImageService.ProfileImageDownload(
                                new InputStreamResource(new ByteArrayInputStream(new byte[0])),
                                MediaType.IMAGE_JPEG,
                                "\"etag\""));

        mockMvc.perform(
                        get("/api/v1/auth/organizations/10/users/42/profile-image/download")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
