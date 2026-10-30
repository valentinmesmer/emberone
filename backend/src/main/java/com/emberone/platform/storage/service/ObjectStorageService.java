/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.storage.service;

import com.emberone.attachment.AllowedAttachmentMediaTypes;
import java.io.InputStream;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

/**
 * Low-level S3 client wrapper. Mutations that must stay consistent with a DB transaction should go
 * through {@link ObjectStorageTransactionCoordinator}; use this type directly for reads and for
 * code that already runs after commit (e.g. {@code @AfterCommitEventListener} with no further DB
 * work).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectStorageService {

    private final S3Client objectStorageClient;

    @Value("${object-storage.profile-images-bucket}")
    private String profileImagesBucket;

    public String profileImagesBucket() {
        return profileImagesBucket;
    }

    private static MediaType resolveProfileImageMediaType(String s3ContentType, String objectKey) {
        if (StringUtils.hasText(s3ContentType)
                && AllowedAttachmentMediaTypes.isAllowedImage(s3ContentType)) {
            try {
                return MediaType.parseMediaType(s3ContentType.toLowerCase());
            } catch (InvalidMediaTypeException ignored) {
            }
        }
        if (objectKey == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        String lower = objectKey.toLowerCase();
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.valueOf("image/webp");
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    /** Creates the bucket if missing; called once at startup, not per upload. */
    public void ensureBucketExists(String bucketName) {
        try {
            objectStorageClient.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (NoSuchBucketException e) {
            objectStorageClient.createBucket(
                    CreateBucketRequest.builder().bucket(bucketName).build());
            log.info("Created object storage bucket: {}", bucketName);
        }
    }

    public String uploadFile(MultipartFile file, String bucketName, String objectName) {
        try {
            String contentType =
                    file.getContentType() == null
                            ? "application/octet-stream"
                            : file.getContentType();
            objectStorageClient.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectName)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.debug("Uploaded object to {}/{}", bucketName, objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Failed to upload file to object storage", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    public String uploadProfileImage(MultipartFile file, Long userId, Long tenantId) {
        String objectName =
                "tenants/"
                        + tenantId
                        + "/users/"
                        + userId
                        + "/profile-image/"
                        + UUID.randomUUID()
                        + getFileExtension(file.getOriginalFilename());
        return uploadFile(file, profileImagesBucket, objectName);
    }

    public InputStream downloadFile(String bucketName, String objectName) {
        try {
            return objectStorageClient.getObject(
                    GetObjectRequest.builder().bucket(bucketName).key(objectName).build());
        } catch (Exception e) {
            log.error("Failed to download file from object storage: {}", objectName, e);
            throw new RuntimeException("Failed to download file", e);
        }
    }

    public ProfileImageObject downloadProfileImageWithContentType(String objectName) {
        try {
            ResponseInputStream<GetObjectResponse> response =
                    objectStorageClient.getObject(
                            GetObjectRequest.builder()
                                    .bucket(profileImagesBucket)
                                    .key(objectName)
                                    .build());
            GetObjectResponse metadata = response.response();
            MediaType mediaType = resolveProfileImageMediaType(metadata.contentType(), objectName);
            return new ProfileImageObject(response, mediaType, metadata.eTag());
        } catch (Exception e) {
            log.error("Failed to download profile image from object storage: {}", objectName, e);
            throw new RuntimeException("Failed to download file", e);
        }
    }

    public void moveFile(String bucketName, String sourceKey, String destinationKey) {
        try {
            copyFile(bucketName, sourceKey, bucketName, destinationKey);
            deleteFile(bucketName, sourceKey);
        } catch (Exception e) {
            log.error(
                    "Failed to move file in object storage: {} -> {}",
                    sourceKey,
                    destinationKey,
                    e);
            throw new RuntimeException("Failed to move file", e);
        }
    }

    public void copyFile(
            String sourceBucket,
            String sourceKey,
            String destinationBucket,
            String destinationKey) {
        try {
            objectStorageClient.copyObject(
                    CopyObjectRequest.builder()
                            .sourceBucket(sourceBucket)
                            .sourceKey(sourceKey)
                            .destinationBucket(destinationBucket)
                            .destinationKey(destinationKey)
                            .build());
            log.debug(
                    "Copied object from {}/{} to {}/{}",
                    sourceBucket,
                    sourceKey,
                    destinationBucket,
                    destinationKey);
        } catch (Exception e) {
            log.error(
                    "Failed to copy file in object storage: {}/{} -> {}/{}",
                    sourceBucket,
                    sourceKey,
                    destinationBucket,
                    destinationKey,
                    e);
            throw new RuntimeException("Failed to copy file", e);
        }
    }

    public void deleteFile(String bucketName, String objectName) {
        try {
            objectStorageClient.deleteObject(
                    DeleteObjectRequest.builder().bucket(bucketName).key(objectName).build());
            log.debug("Deleted object from {}/{}", bucketName, objectName);
        } catch (Exception e) {
            log.error("Failed to delete file from object storage: {}", objectName, e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    public void deleteObjectsByPrefix(String bucketName, String keyPrefix) {
        try {
            String continuationToken = null;
            do {
                ListObjectsV2Request.Builder requestBuilder =
                        ListObjectsV2Request.builder().bucket(bucketName).prefix(keyPrefix);
                if (continuationToken != null) {
                    requestBuilder.continuationToken(continuationToken);
                }
                ListObjectsV2Response response =
                        objectStorageClient.listObjectsV2(requestBuilder.build());
                for (S3Object object : response.contents()) {
                    deleteFile(bucketName, object.key());
                }
                continuationToken =
                        response.isTruncated() ? response.nextContinuationToken() : null;
            } while (continuationToken != null);
        } catch (Exception e) {
            log.error(
                    "Failed to delete objects by prefix from object storage: {}/{}",
                    bucketName,
                    keyPrefix,
                    e);
            throw new RuntimeException("Failed to delete objects by prefix", e);
        }
    }

    public void deleteProfileImage(String objectName) {
        deleteFile(profileImagesBucket, objectName);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex);
    }

    public record ProfileImageObject(InputStream inputStream, MediaType contentType, String etag) {}
}
