/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.storage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ensures the configured buckets exist once per start, so uploads and copies skip the per-call
 * {@code headBucket} round trip. A failure is logged and does not block startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ObjectStorageBucketInitializer {

    private final ObjectStorageService objectStorageService;
    private final ObjectStorageBucketCatalog bucketCatalog;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureBuckets() {
        for (String bucket : bucketCatalog.tenantScopedBuckets()) {
            try {
                objectStorageService.ensureBucketExists(bucket);
            } catch (Exception e) {
                log.error("Could not ensure object storage bucket {}", bucket, e);
            }
        }
    }
}
