/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.profileimage.controller;

import com.emberone.profileimage.service.ProfileImageService;
import com.emberone.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/users/{userId}/profile-image")
@RequiredArgsConstructor
public class ProfileImageController {

    private final ProfileImageService profileImageService;
    private final UserService userService;

    @GetMapping("/download")
    @PreAuthorize("@securityService.hasProfileImageReadPermission(#organizationId)")
    public ResponseEntity<InputStreamResource> downloadProfileImage(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        ProfileImageService.ProfileImageDownload download =
                profileImageService.downloadProfileImage(userId);
        return ProfileImageDownloadResponses.ok(download);
    }
}
