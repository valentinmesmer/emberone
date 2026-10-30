/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@ExtendWith(MockitoExtension.class)
class ObjectStorageServiceTest {

    @Mock S3Client client;
    private ObjectStorageService service;

    @BeforeEach
    void setUp() {
        service = new ObjectStorageService(client);
        ReflectionTestUtils.setField(service, "profileImagesBucket", "profiles");
    }

    @Test
    void missingBucketIsCreated() {
        when(client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(NoSuchBucketException.builder().build());

        service.ensureBucketExists("documents");

        verify(client)
                .createBucket(
                        org.mockito.ArgumentMatchers.<CreateBucketRequest>argThat(
                                r -> r.bucket().equals("documents")));
    }

    @Test
    void existingBucketIsNotRecreated() {
        service.ensureBucketExists("documents");

        verify(client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void uploadSetsBucketKeyAndContentType() {
        MockMultipartFile file =
                new MockMultipartFile("file", "a.pdf", "application/pdf", "data".getBytes());

        assertThat(service.uploadFile(file, "documents", "tenant/a.pdf")).isEqualTo("tenant/a.pdf");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client)
                .putObject(
                        captor.capture(), any(software.amazon.awssdk.core.sync.RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo("documents");
        assertThat(captor.getValue().key()).isEqualTo("tenant/a.pdf");
        assertThat(captor.getValue().contentType()).isEqualTo("application/pdf");
    }

    @Test
    void moveCopiesBeforeDeletingSource() {
        service.moveFile("documents", "old", "new");

        var order = inOrder(client);
        order.verify(client).copyObject(any(CopyObjectRequest.class));
        order.verify(client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void copyAndDeleteFailuresAreWrapped() {
        when(client.copyObject(any(CopyObjectRequest.class)))
                .thenThrow(new RuntimeException("copy failed"));
        assertThatThrownBy(() -> service.copyFile("a", "old", "b", "new"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to copy file");

        when(client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(new RuntimeException("delete failed"));
        assertThatThrownBy(() -> service.deleteFile("a", "old"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to delete file");
    }

    @Test
    void profileImageKeyContainsTenantUserAndExtension() {
        MockMultipartFile file =
                new MockMultipartFile("file", "photo.webp", "image/webp", "data".getBytes());

        String key = service.uploadProfileImage(file, 1L, 7L);

        assertThat(key).startsWith("tenants/7/users/1/profile-image/").endsWith(".webp");
    }

    @Test
    void downloadStreamsObjectAndWrapsS3Failures() throws Exception {
        ResponseInputStream<GetObjectResponse> stream =
                new ResponseInputStream<>(
                        GetObjectResponse.builder().contentType("application/pdf").build(),
                        new java.io.ByteArrayInputStream("data".getBytes()));
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(stream);

        assertThat(service.downloadFile("documents", "a.pdf").readAllBytes())
                .isEqualTo("data".getBytes());

        when(client.getObject(any(GetObjectRequest.class)))
                .thenThrow(new RuntimeException("s3 down"));
        assertThatThrownBy(() -> service.downloadFile("documents", "a.pdf"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to download file");
    }

    @Test
    void profileDownloadUsesContentTypeAndFallsBackToExtension() {
        stubProfileObject("photo.png", "invalid type", "png".getBytes());

        ObjectStorageService.ProfileImageObject result =
                service.downloadProfileImageWithContentType("photo.png");

        assertThat(result.contentType()).isEqualTo(org.springframework.http.MediaType.IMAGE_PNG);
        assertThat(result.etag()).isEqualTo("\"stub-etag\"");
    }

    @Test
    void uploadUsesOctetStreamWhenContentTypeMissing() {
        MockMultipartFile file = new MockMultipartFile("file", "a.bin", null, "data".getBytes());

        service.uploadFile(file, "documents", "tenant/a.bin");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client)
                .putObject(
                        captor.capture(), any(software.amazon.awssdk.core.sync.RequestBody.class));
        assertThat(captor.getValue().contentType()).isEqualTo("application/octet-stream");
    }

    @Test
    void uploadFailureIsWrapped() {
        when(client.putObject(
                        any(PutObjectRequest.class),
                        any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenThrow(new RuntimeException("s3 down"));
        MockMultipartFile file =
                new MockMultipartFile("file", "a.pdf", "application/pdf", "data".getBytes());

        assertThatThrownBy(() -> service.uploadFile(file, "documents", "tenant/a.pdf"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to upload file");
    }

    @Test
    void moveFailureIsWrapped() {
        when(client.copyObject(any(CopyObjectRequest.class)))
                .thenThrow(new RuntimeException("move failed"));

        assertThatThrownBy(() -> service.moveFile("documents", "old", "new"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to move file");
    }

    @Test
    void profileImageKeyUsesEmptyExtensionWhenFilenameMissing() {
        MockMultipartFile file =
                new MockMultipartFile("file", null, "image/jpeg", "data".getBytes());

        String key = service.uploadProfileImage(file, 1L, 7L);

        assertThat(key).startsWith("tenants/7/users/1/profile-image/").doesNotContain(".");
    }

    @Test
    void profileDownloadResolvesGifWebpJpegAndValidS3ContentType() {
        assertProfileMediaType(
                "photo.gif", "invalid", org.springframework.http.MediaType.IMAGE_GIF);
        assertProfileMediaType(
                "photo.webp", null, org.springframework.http.MediaType.valueOf("image/webp"));
        assertProfileMediaType("photo.jpeg", null, org.springframework.http.MediaType.IMAGE_JPEG);
        assertProfileMediaType("photo.jpg", null, org.springframework.http.MediaType.IMAGE_JPEG);
        assertProfileMediaType(
                "photo.bin", "image/png", org.springframework.http.MediaType.IMAGE_PNG);
        assertProfileMediaType(
                "evil.svg",
                "image/svg+xml",
                org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
        assertProfileMediaType(
                null, null, org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
    }

    @Test
    void deleteProfileImageDelegatesToBucket() {
        service.deleteProfileImage("tenant/1/profile.jpg");
        verify(client)
                .deleteObject(
                        org.mockito.ArgumentMatchers.<DeleteObjectRequest>argThat(
                                r ->
                                        r.bucket().equals("profiles")
                                                && r.key().equals("tenant/1/profile.jpg")));
    }

    private void assertProfileMediaType(
            String objectKey, String s3ContentType, MediaType expected) {
        stubProfileObject(objectKey, s3ContentType, "data".getBytes());

        ObjectStorageService.ProfileImageObject result =
                service.downloadProfileImageWithContentType(objectKey);

        assertThat(result.contentType()).isEqualTo(expected);
    }

    private void stubProfileObject(String objectKey, String s3ContentType, byte[] data) {
        GetObjectResponse metadata =
                GetObjectResponse.builder()
                        .contentType(s3ContentType)
                        .eTag("\"stub-etag\"")
                        .build();
        ResponseInputStream<GetObjectResponse> stream =
                new ResponseInputStream<>(
                        metadata, AbortableInputStream.create(new ByteArrayInputStream(data)));
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(stream);
    }
}
