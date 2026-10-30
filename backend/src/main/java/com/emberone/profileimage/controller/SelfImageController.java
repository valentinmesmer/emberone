/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.profileimage.controller;

import com.emberone.platform.security.TokenService;
import com.emberone.profileimage.service.ProfileImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/self/profile-image")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class SelfImageController {

    private final ProfileImageService profileImageService;
    private final TokenService tokenService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        String objectName =
                profileImageService.uploadProfileImage(tokenService.requireUserId(), file);
        return ResponseEntity.ok(objectName);
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadProfileImage() {
        ProfileImageService.ProfileImageDownload download =
                profileImageService.downloadProfileImage(tokenService.requireUserId());
        return ProfileImageDownloadResponses.ok(download);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteProfileImage() {
        profileImageService.deleteProfileImage(tokenService.requireUserId());
        return ResponseEntity.noContent().build();
    }
}
