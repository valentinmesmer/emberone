/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.storage.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** All object-storage buckets that store tenant-scoped keys under {@code tenants/{tenantId}/}. */
@Component
@RequiredArgsConstructor
public class ObjectStorageBucketCatalog {

    private final ObjectStorageService objectStorageService;

    @Value("${object-storage.user-documents-bucket}")
    private String userDocumentsBucket;

    @Value("${object-storage.workflows-bucket}")
    private String workflowsBucket;

    @Value("${object-storage.attachments-bucket}")
    private String attachmentsBucket;

    public List<String> tenantScopedBuckets() {
        return List.of(
                userDocumentsBucket,
                objectStorageService.profileImagesBucket(),
                workflowsBucket,
                attachmentsBucket);
    }
}
