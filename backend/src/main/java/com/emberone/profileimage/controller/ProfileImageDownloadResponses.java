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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

final class ProfileImageDownloadResponses {

    private static final CacheControl CACHE_CONTROL = CacheControl.noCache().cachePrivate();

    private ProfileImageDownloadResponses() {}

    /**
     * Answers {@code 304} from the stored entity tag without reading the image from object storage;
     * otherwise streams the image. The cache stays {@code private, no-cache}: the browser
     * revalidates on every use, which is cheap now and picks up replaced images immediately.
     */
    static ResponseEntity<InputStreamResource> respond(
            ProfileImageService service, Long userId, WebRequest request) {
        String etag = service.profileImageEtag(userId);
        if (request.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .cacheControl(CACHE_CONTROL)
                    .eTag(etag)
                    .build();
        }
        ProfileImageService.ProfileImageDownload download = service.downloadProfileImage(userId);
        return ResponseEntity.ok()
                .cacheControl(CACHE_CONTROL)
                .eTag(download.etag())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"profile-image\"")
                .contentType(download.contentType())
                .body(download.resource());
    }
}
