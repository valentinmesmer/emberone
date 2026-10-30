/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.profileimage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.platform.security.TenantContextHolder;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.storage.service.ObjectStorageDeletionOutboxService;
import com.emberone.platform.storage.service.ObjectStorageService;
import com.emberone.platform.storage.service.ObjectStorageTransactionCoordinator;
import com.emberone.profileimage.domain.UserProfileImage;
import com.emberone.profileimage.exception.ProfileImageNotFoundException;
import com.emberone.profileimage.repository.UserProfileImageRepository;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ProfileImageServiceTest {

    @Mock ObjectStorageService storageService;
    @Mock ObjectStorageDeletionOutboxService deletionOutboxService;
    @Mock UserProfileImageRepository repository;
    @Mock TokenService tokenService;
    private ProfileImageService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(7L);
        lenient().when(storageService.profileImagesBucket()).thenReturn("profiles");
        service =
                new ProfileImageService(
                        storageService,
                        new ObjectStorageTransactionCoordinator(
                                storageService, deletionOutboxService),
                        repository,
                        tokenService);
    }

    @Test
    void uploadReplacesOldObjectAndPersistsNewTenantScopedKey() {
        UserProfileImage image =
                UserProfileImage.builder()
                        .userId(1L)
                        .tenantId(7L)
                        .profileImageUrl("tenants/7/users/1/profile-image/old.png")
                        .build();
        MockMultipartFile file =
                new MockMultipartFile("file", "photo.png", "image/png", "image".getBytes());
        when(tokenService.getTenantId()).thenReturn(7L);
        when(repository.findById(1L)).thenReturn(Optional.of(image));
        when(storageService.uploadProfileImage(file, 1L, 7L))
                .thenReturn("tenants/7/users/1/profile-image/new.png");

        assertThat(service.uploadProfileImage(1L, file))
                .isEqualTo("tenants/7/users/1/profile-image/new.png");
        verify(deletionOutboxService)
                .enqueueObjectDeletion(7L, "profiles", "tenants/7/users/1/profile-image/old.png");
        assertThat(image.getProfileImageUrl()).endsWith("new.png");
        verify(repository).save(image);
    }

    @Test
    void oldImageCleanupIsEnqueuedWithoutBlockingReplacement() {
        UserProfileImage image =
                UserProfileImage.builder()
                        .userId(1L)
                        .tenantId(7L)
                        .profileImageUrl("tenants/7/users/1/profile-image/old.png")
                        .build();
        MockMultipartFile file =
                new MockMultipartFile("file", "photo.png", "image/png", "image".getBytes());
        when(tokenService.getTenantId()).thenReturn(7L);
        when(repository.findById(1L)).thenReturn(Optional.of(image));
        when(storageService.uploadProfileImage(file, 1L, 7L))
                .thenReturn("tenants/7/users/1/profile-image/new.png");

        assertThat(service.uploadProfileImage(1L, file))
                .isEqualTo("tenants/7/users/1/profile-image/new.png");
        verify(deletionOutboxService)
                .enqueueObjectDeletion(7L, "profiles", "tenants/7/users/1/profile-image/old.png");
        verify(repository).save(image);
    }

    @Test
    void uploadRejectsSvgAndNonRasterImages() {
        when(tokenService.getTenantId()).thenReturn(7L);
        MockMultipartFile svg =
                new MockMultipartFile(
                        "file", "evil.svg", "image/svg+xml", "<svg></svg>".getBytes());
        assertThatThrownBy(() -> service.uploadProfileImage(1L, svg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("raster image");

        MockMultipartFile emptyType =
                new MockMultipartFile(
                        "file", "photo.bin", "application/octet-stream", "x".getBytes());
        assertThatThrownBy(() -> service.uploadProfileImage(1L, emptyType))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void downloadRejectsCrossTenantAndForgedObjectKey() {
        when(tokenService.getTenantId()).thenReturn(7L);
        when(repository.findById(1L))
                .thenReturn(
                        Optional.of(
                                UserProfileImage.builder()
                                        .userId(1L)
                                        .tenantId(8L)
                                        .profileImageUrl("key")
                                        .build()));
        assertThatThrownBy(() -> service.downloadProfileImage(1L))
                .isInstanceOf(SecurityException.class);

        when(repository.findById(1L))
                .thenReturn(
                        Optional.of(
                                UserProfileImage.builder()
                                        .userId(1L)
                                        .tenantId(7L)
                                        .profileImageUrl(
                                                "tenants/7/users/2/profile-image/stolen.png")
                                        .build()));
        assertThatThrownBy(() -> service.downloadProfileImage(1L))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void downloadReturnsStorageContentTypeAndDeleteClearsUrl() {
        String key = "tenants/7/users/1/profile-image/photo.png";
        UserProfileImage image =
                UserProfileImage.builder().userId(1L).tenantId(7L).profileImageUrl(key).build();
        when(tokenService.getTenantId()).thenReturn(7L);
        when(repository.findById(1L)).thenReturn(Optional.of(image));
        when(storageService.downloadProfileImageWithContentType(key))
                .thenReturn(
                        new ObjectStorageService.ProfileImageObject(
                                new ByteArrayInputStream("image".getBytes()),
                                MediaType.IMAGE_PNG,
                                "\"etag\""));

        assertThat(service.downloadProfileImage(1L).contentType()).isEqualTo(MediaType.IMAGE_PNG);
        service.deleteProfileImage(1L);
        verify(deletionOutboxService).enqueueObjectDeletion(7L, "profiles", key);
        assertThat(image.getProfileImageUrl()).isNull();
    }

    @Test
    void missingImageIsReported() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.downloadProfileImage(1L))
                .isInstanceOf(ProfileImageNotFoundException.class);
    }
}
