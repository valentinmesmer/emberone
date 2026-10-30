/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.profileimage.service;

import com.emberone.attachment.AllowedAttachmentMediaTypes;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.storage.service.ObjectStorageService;
import com.emberone.platform.storage.service.ObjectStorageTransactionCoordinator;
import com.emberone.profileimage.domain.UserProfileImage;
import com.emberone.profileimage.exception.ProfileImageNotFoundException;
import com.emberone.profileimage.repository.UserProfileImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private final ObjectStorageService objectStorageService;
    private final ObjectStorageTransactionCoordinator objectStorageTransactionCoordinator;
    private final UserProfileImageRepository userProfileImageRepository;
    private final TokenService tokenService;

    @Transactional
    public String uploadProfileImage(Long userId, MultipartFile file) {
        Long tenantId = requireTenantId();
        // Validate file type
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (!AllowedAttachmentMediaTypes.isAllowedImage(contentType)) {
            throw new IllegalArgumentException(
                    "File must be a raster image (JPEG, PNG, GIF, or WebP)");
        }

        // Validate file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must be less than 10MB");
        }

        // Get or create user profile image
        UserProfileImage userProfileImage =
                userProfileImageRepository
                        .findById(userId)
                        .orElseGet(
                                () -> {
                                    log.debug(
                                            "Creating new profile image entry for user {}", userId);
                                    return UserProfileImage.builder()
                                            .userId(userId)
                                            .tenantId(tenantId)
                                            .build();
                                });
        if (userProfileImage.getTenantId() == null) {
            userProfileImage.setTenantId(tenantId);
        }

        String oldImageUrl = userProfileImage.getProfileImageUrl();

        String objectName =
                objectStorageTransactionCoordinator.uploadProfileImageWithRollbackCleanup(
                        file, userId, tenantId);
        log.info("Uploaded profile image for user {}", userId);

        userProfileImage.setProfileImageUrl(objectName);
        userProfileImageRepository.save(userProfileImage);

        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            objectStorageTransactionCoordinator.deleteProfileImageAfterCommit(oldImageUrl);
            log.info("Scheduled removal of previous profile image for user {}", userId);
        }

        return objectName;
    }

    public ProfileImageDownload downloadProfileImage(Long userId) {
        UserProfileImage userProfileImage =
                userProfileImageRepository
                        .findById(userId)
                        .orElseThrow(() -> new ProfileImageNotFoundException(userId));
        Long tenantId = requireTenantId();
        if (!tenantId.equals(userProfileImage.getTenantId())) {
            throw new SecurityException("Unauthorized access to profile image");
        }

        String objectName = userProfileImage.getProfileImageUrl();

        if (objectName == null || objectName.isEmpty()) {
            throw new ProfileImageNotFoundException(userId);
        }

        // Verify that the object belongs to this user
        if (!objectName.startsWith(
                "tenants/" + tenantId + "/users/" + userId + "/profile-image/")) {
            log.warn("User {} attempted unauthorized profile image access", userId);
            throw new SecurityException("Unauthorized access to profile image");
        }

        ObjectStorageService.ProfileImageObject object =
                objectStorageService.downloadProfileImageWithContentType(objectName);
        return new ProfileImageDownload(
                new InputStreamResource(object.inputStream()), object.contentType(), object.etag());
    }

    @Transactional
    public void deleteProfileImage(Long userId) {
        UserProfileImage userProfileImage =
                userProfileImageRepository.findById(userId).orElse(null);
        Long tenantId = requireTenantId();

        if (userProfileImage == null
                || userProfileImage.getProfileImageUrl() == null
                || userProfileImage.getProfileImageUrl().isEmpty()) {
            return; // No image to delete
        }
        if (!tenantId.equals(userProfileImage.getTenantId())) {
            throw new SecurityException("Unauthorized access to profile image");
        }

        String objectName = userProfileImage.getProfileImageUrl();

        // Verify that the object belongs to this user
        if (!objectName.startsWith(
                "tenants/" + tenantId + "/users/" + userId + "/profile-image/")) {
            log.warn("User {} attempted unauthorized profile image deletion", userId);
            throw new SecurityException("Unauthorized access to profile image");
        }

        userProfileImage.setProfileImageUrl(null);
        userProfileImageRepository.save(userProfileImage);

        objectStorageTransactionCoordinator.deleteProfileImageAfterCommit(objectName);
        log.info("Scheduled profile image deletion for user {}", userId);
    }

    private Long requireTenantId() {
        Long tenantId = tokenService.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenantId found in token");
        }
        return tenantId;
    }

    public record ProfileImageDownload(
            InputStreamResource resource, MediaType contentType, String etag) {}
}
