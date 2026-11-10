/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.profileimage.controller;

import com.emberone.profileimage.service.ProfileImageService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

final class ProfileImageDownloadResponses {

    private ProfileImageDownloadResponses() {}

    static ResponseEntity<InputStreamResource> ok(
            ProfileImageService.ProfileImageDownload download) {
        ResponseEntity.BodyBuilder builder =
                ResponseEntity.ok()
                        .cacheControl(CacheControl.noCache().cachePrivate())
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
