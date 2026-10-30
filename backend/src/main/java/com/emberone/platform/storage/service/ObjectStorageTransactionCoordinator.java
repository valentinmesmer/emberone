/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.storage.service;

import com.emberone.platform.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Couples irreversible object-storage mutations to Spring transaction outcomes so DB rollbacks do
 * not leave orphaned metadata or orphaned blobs. S3 deletes are enqueued in {@code
 * obj_pending_deletions} and processed asynchronously with retry.
 */
@Service
@RequiredArgsConstructor
public class ObjectStorageTransactionCoordinator {

    private final ObjectStorageService objectStorageService;
    private final ObjectStorageDeletionOutboxService deletionOutboxService;

    public void uploadFileWithRollbackCleanup(
            MultipartFile file, String bucketName, String objectName) {
        objectStorageService.uploadFile(file, bucketName, objectName);
        enqueueDeletionOnRollback(TenantContextHolder.getTenantId(), bucketName, objectName);
    }

    public void copyFileWithRollbackCleanup(
            String sourceBucket,
            String sourceKey,
            String destinationBucket,
            String destinationKey) {
        objectStorageService.copyFile(sourceBucket, sourceKey, destinationBucket, destinationKey);
        enqueueDeletionOnRollback(
                TenantContextHolder.getTenantId(), destinationBucket, destinationKey);
    }

    /** Enqueues the delete in the current transaction; a rollback discards it with the rest. */
    public void deleteFileAfterCommit(String bucketName, String objectName) {
        deletionOutboxService.enqueueObjectDeletion(requireTenantId(), bucketName, objectName);
    }

    public String uploadProfileImageWithRollbackCleanup(
            MultipartFile file, Long userId, Long tenantId) {
        String objectName = objectStorageService.uploadProfileImage(file, userId, tenantId);
        enqueueDeletionOnRollback(tenantId, objectStorageService.profileImagesBucket(), objectName);
        return objectName;
    }

    public void deleteProfileImageAfterCommit(String objectName) {
        deleteFileAfterCommit(objectStorageService.profileImagesBucket(), objectName);
    }

    private static Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException(
                    "Tenant context is required for object storage deletes");
        }
        return tenantId;
    }

    private void enqueueDeletionOnRollback(Long tenantId, String bucketName, String objectName) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            deletionOutboxService.enqueueObjectDeletionInNewTransaction(
                                    tenantId, bucketName, objectName);
                        }
                    }
                });
    }
}
