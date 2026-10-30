/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.storage.service;

import com.emberone.platform.storage.config.ObjectStorageProperties;
import com.emberone.platform.storage.domain.ObjectPendingDeletion;
import com.emberone.platform.storage.domain.ObjectPendingDeletionStatus;
import com.emberone.platform.storage.repository.ObjectPendingDeletionRepository;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectStorageDeletionOutboxService {

    private static final Pattern TENANT_PREFIX = Pattern.compile("^tenants/(\\d+)(?:/|$)");

    private final ObjectPendingDeletionRepository pendingDeletionRepository;
    private final ObjectStorageDeletionOutboxTransactionService transactionService;
    private final ObjectStorageService objectStorageService;
    private final ObjectStorageProperties properties;

    @Transactional
    public void enqueueObjectDeletion(Long tenantId, String bucketName, String objectKey) {
        requireTenant(tenantId);
        requireText(bucketName, "bucketName");
        requireText(objectKey, "objectKey");
        assertTenantScopedKey(tenantId, objectKey);
        pendingDeletionRepository.save(
                ObjectPendingDeletion.builder()
                        .tenantId(tenantId)
                        .bucketName(bucketName)
                        .objectKey(objectKey)
                        .status(ObjectPendingDeletionStatus.PENDING)
                        .build());
    }

    @Transactional
    public void enqueuePrefixDeletion(Long tenantId, String bucketName, String keyPrefix) {
        requireTenant(tenantId);
        requireText(bucketName, "bucketName");
        requireText(keyPrefix, "keyPrefix");
        assertTenantScopedKey(tenantId, keyPrefix);
        pendingDeletionRepository.save(
                ObjectPendingDeletion.builder()
                        .tenantId(tenantId)
                        .bucketName(bucketName)
                        .keyPrefix(keyPrefix)
                        .status(ObjectPendingDeletionStatus.PENDING)
                        .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueueObjectDeletionInNewTransaction(
            Long tenantId, String bucketName, String objectKey) {
        enqueueObjectDeletion(tenantId, bucketName, objectKey);
    }

    public void processPendingDeletions() {
        processPendingDeletions(false);
    }

    /** Processes outbox rows left after tenant purge ({@code tenant_id} cleared by FK SET NULL). */
    public void processPendingDeletionsForDetachedTenants() {
        processPendingDeletions(true);
    }

    private void processPendingDeletions(boolean detachedTenants) {
        Instant now = Instant.now();
        Instant staleBefore = now.minus(properties.getProcessingStaleAfter());
        int batchSize = properties.getDeletionOutboxBatchSize();
        List<Long> claimedIds =
                detachedTenants
                        ? transactionService.claimPendingDeletionIdsForDetachedTenants(
                                now, staleBefore, batchSize)
                        : transactionService.claimPendingDeletionIds(now, staleBefore, batchSize);
        if (claimedIds.isEmpty()) {
            return;
        }
        for (Long deletionId : claimedIds) {
            processClaimedDeletion(deletionId, detachedTenants);
        }
    }

    private void processClaimedDeletion(Long deletionId, boolean platformScope) {
        ObjectPendingDeletion entry =
                transactionService.loadClaimedDeletion(deletionId, platformScope);
        if (entry == null || entry.getStatus() != ObjectPendingDeletionStatus.IN_PROGRESS) {
            return;
        }
        boolean rowNeedsPlatformScope = platformScope || entry.getTenantId() == null;
        int attempt = entry.getAttempts() + 1;
        try {
            Long tenantIdForKey = resolveTenantIdForKey(entry);
            if (StringUtils.hasText(entry.getObjectKey())) {
                assertTenantScopedKey(tenantIdForKey, entry.getObjectKey());
                objectStorageService.deleteFile(entry.getBucketName(), entry.getObjectKey());
            } else if (StringUtils.hasText(entry.getKeyPrefix())) {
                assertTenantScopedKey(tenantIdForKey, entry.getKeyPrefix());
                objectStorageService.deleteObjectsByPrefix(
                        entry.getBucketName(), entry.getKeyPrefix());
            } else {
                transactionService.markFailed(
                        deletionId,
                        attempt,
                        "Missing object key or prefix",
                        false,
                        rowNeedsPlatformScope);
                return;
            }
            transactionService.deleteCompleted(deletionId, rowNeedsPlatformScope);
        } catch (Exception ex) {
            log.warn("Object storage deletion {} failed on attempt {}", deletionId, attempt, ex);
            boolean retryable = attempt < properties.getMaxDeletionAttempts();
            transactionService.markFailed(
                    deletionId, attempt, ex.getMessage(), retryable, rowNeedsPlatformScope);
            if (!retryable) {
                log.error(
                        "Object storage deletion {} permanently failed after {} attempts: {}",
                        deletionId,
                        attempt,
                        ex.getMessage());
            }
        }
    }

    private static void requireTenant(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalStateException("Tenant id is required for object deletion outbox");
        }
    }

    private static void requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static void assertTenantScopedKey(Long tenantId, String keyOrPrefix) {
        String requiredPrefix = "tenants/" + tenantId + "/";
        if (!keyOrPrefix.startsWith(requiredPrefix)) {
            throw new IllegalArgumentException(
                    "Object storage key must start with " + requiredPrefix);
        }
    }

    private static Long resolveTenantIdForKey(ObjectPendingDeletion entry) {
        if (entry.getTenantId() != null) {
            return entry.getTenantId();
        }
        String keyOrPrefix =
                StringUtils.hasText(entry.getKeyPrefix())
                        ? entry.getKeyPrefix()
                        : entry.getObjectKey();
        if (!StringUtils.hasText(keyOrPrefix)) {
            throw new IllegalStateException("Missing object key or prefix");
        }
        Matcher matcher = TENANT_PREFIX.matcher(keyOrPrefix);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Object storage key must start with tenants/{id}/");
        }
        return Long.parseLong(matcher.group(1));
    }
}
