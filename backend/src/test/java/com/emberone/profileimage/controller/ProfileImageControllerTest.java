/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.profileimage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.profileimage.service.ProfileImageService;
import com.emberone.user.service.UserService;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ProfileImageControllerTest {

    @Mock ProfileImageService profileImageService;
    @Mock UserService userService;

    @InjectMocks ProfileImageController controller;

    @Test
    void downloadProfileImageReturnsInlineDisposition() {
        InputStreamResource resource =
                new InputStreamResource(new ByteArrayInputStream(new byte[0]));
        ProfileImageService.ProfileImageDownload download =
                new ProfileImageService.ProfileImageDownload(
                        resource, MediaType.IMAGE_PNG, "\"etag\"");
        when(profileImageService.downloadProfileImage(9L)).thenReturn(download);

        ResponseEntity<InputStreamResource> response = controller.downloadProfileImage(10L, 9L);

        verify(userService).assertUserBelongsToOrganization(9L, 10L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("inline");
        assertThat(response.getHeaders().getCacheControl()).contains("max-age");
        assertThat(response.getHeaders().getETag()).isEqualTo("\"etag\"");
        assertThat(response.getBody()).isEqualTo(resource);
    }
}
