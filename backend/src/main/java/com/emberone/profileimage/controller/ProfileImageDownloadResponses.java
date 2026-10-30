/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.profileimage.controller;

import com.emberone.profileimage.service.ProfileImageService;
import java.time.Duration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

final class ProfileImageDownloadResponses {

    private static final Duration CACHE_MAX_AGE = Duration.ofDays(7);

    private ProfileImageDownloadResponses() {}

    static ResponseEntity<InputStreamResource> ok(
            ProfileImageService.ProfileImageDownload download) {
        ResponseEntity.BodyBuilder builder =
                ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(CACHE_MAX_AGE).cachePrivate())
                        .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"profile-image\"")
                        .contentType(download.contentType());
        if (download.etag() != null) {
            builder.eTag(download.etag());
        }
        return builder.body(download.resource());
    }
}
