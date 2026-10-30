/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.profileimage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.platform.security.TokenService;
import com.emberone.profileimage.service.ProfileImageService;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class SelfImageControllerTest {

    @Mock ProfileImageService profileImageService;
    @Mock TokenService tokenService;
    @Mock MultipartFile file;

    SelfImageController controller;

    @BeforeEach
    void setUp() {
        controller = new SelfImageController(profileImageService, tokenService);
    }

    @Test
    void uploadProfileImageDelegatesToService() {
        when(tokenService.requireUserId()).thenReturn(4L);
        when(profileImageService.uploadProfileImage(4L, file)).thenReturn("profile.png");

        ResponseEntity<String> response = controller.uploadProfileImage(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("profile.png");
    }

    @Test
    void deleteProfileImageReturnsNoContent() {
        when(tokenService.requireUserId()).thenReturn(4L);

        ResponseEntity<Void> response = controller.deleteProfileImage();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(profileImageService).deleteProfileImage(4L);
    }

    @Test
    void downloadProfileImageReturnsResourceForAuthenticatedUser() {
        when(tokenService.requireUserId()).thenReturn(4L);
        InputStreamResource resource =
                new InputStreamResource(new ByteArrayInputStream(new byte[0]));
        when(profileImageService.downloadProfileImage(4L))
                .thenReturn(
                        new ProfileImageService.ProfileImageDownload(
                                resource, MediaType.IMAGE_JPEG, "\"etag\""));

        ResponseEntity<InputStreamResource> response = controller.downloadProfileImage();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resource);
    }
}
