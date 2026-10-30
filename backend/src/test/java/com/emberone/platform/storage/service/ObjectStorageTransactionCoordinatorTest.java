/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.storage.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emberone.platform.security.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class ObjectStorageTransactionCoordinatorTest {

    private static final long TENANT_ID = 7L;
    private static final String TENANT_KEY = "tenants/7/objects/file";
    private static final String TENANT_DEST = "tenants/7/objects/dest";
    private static final String TENANT_SOURCE = "tenants/7/objects/source";
    private static final String TENANT_NEW = "tenants/7/objects/new-key";
    private static final String TENANT_IMG = "tenants/7/users/42/profile-image/img.png";

    @Mock ObjectStorageService objectStorageService;
    @Mock ObjectStorageDeletionOutboxService deletionOutboxService;

    private ObjectStorageTransactionCoordinator coordinator;

    @BeforeEach
    void setUp() {
        coordinator =
                new ObjectStorageTransactionCoordinator(
                        objectStorageService, deletionOutboxService);
        TenantContextHolder.setTenantId(TENANT_ID);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void deleteFileAfterCommitEnqueuesInCurrentTransaction() {
        coordinator.deleteFileAfterCommit("bucket", TENANT_KEY);

        verify(deletionOutboxService).enqueueObjectDeletion(TENANT_ID, "bucket", TENANT_KEY);
        verify(deletionOutboxService, never())
                .enqueueObjectDeletionInNewTransaction(TENANT_ID, "bucket", TENANT_KEY);
        verifyNoInteractions(objectStorageService);
    }

    @Test
    void deleteFileAfterCommitDoesNotEnqueueWhenTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();

        coordinator.deleteFileAfterCommit("bucket", TENANT_KEY);
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(deletionOutboxService).enqueueObjectDeletion(TENANT_ID, "bucket", TENANT_KEY);
        verifyNoInteractions(objectStorageService);
    }

    @Test
    void uploadRollbackEnqueuesOnlyUploadedObject() {
        TransactionSynchronizationManager.initSynchronization();
        MockMultipartFile file =
                new MockMultipartFile("file", "a.pdf", "application/pdf", "data".getBytes());

        coordinator.uploadFileWithRollbackCleanup(file, "bucket", TENANT_NEW);
        verify(objectStorageService).uploadFile(file, "bucket", TENANT_NEW);
        verifyNoInteractions(deletionOutboxService);

        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(deletionOutboxService)
                .enqueueObjectDeletionInNewTransaction(TENANT_ID, "bucket", TENANT_NEW);
    }

    @Test
    void copyRollbackEnqueuesOnlyDestinationCopy() {
        TransactionSynchronizationManager.initSynchronization();

        coordinator.copyFileWithRollbackCleanup("bucket", TENANT_SOURCE, "bucket", TENANT_DEST);
        verify(objectStorageService).copyFile("bucket", TENANT_SOURCE, "bucket", TENANT_DEST);
        verifyNoInteractions(deletionOutboxService);

        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(deletionOutboxService)
                .enqueueObjectDeletionInNewTransaction(TENANT_ID, "bucket", TENANT_DEST);
        verify(deletionOutboxService, never())
                .enqueueObjectDeletionInNewTransaction(TENANT_ID, "bucket", TENANT_SOURCE);
    }

    @Test
    void moveReplacementCopiesThenEnqueuesSourceDeletion() {
        TransactionSynchronizationManager.initSynchronization();

        coordinator.copyFileWithRollbackCleanup("bucket", TENANT_SOURCE, "bucket", TENANT_DEST);
        coordinator.deleteFileAfterCommit("bucket", TENANT_SOURCE);

        verify(objectStorageService).copyFile("bucket", TENANT_SOURCE, "bucket", TENANT_DEST);
        verify(deletionOutboxService).enqueueObjectDeletion(TENANT_ID, "bucket", TENANT_SOURCE);
        verify(objectStorageService, never()).moveFile(any(), any(), any());
    }

    @Test
    void profileImageUploadRollbackUsesProvidedTenant() {
        TransactionSynchronizationManager.initSynchronization();
        MockMultipartFile file =
                new MockMultipartFile("file", "a.png", "image/png", "data".getBytes());
        when(objectStorageService.uploadProfileImage(file, 42L, TENANT_ID)).thenReturn(TENANT_IMG);
        when(objectStorageService.profileImagesBucket()).thenReturn("profiles");

        coordinator.uploadProfileImageWithRollbackCleanup(file, 42L, TENANT_ID);
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(deletionOutboxService)
                .enqueueObjectDeletionInNewTransaction(TENANT_ID, "profiles", TENANT_IMG);
    }

    private static void completeTransaction(int status) {
        for (TransactionSynchronization synchronization :
                TransactionSynchronizationManager.getSynchronizations()) {
            if (status == TransactionSynchronization.STATUS_COMMITTED) {
                synchronization.afterCommit();
            }
            synchronization.afterCompletion(status);
        }
        TransactionSynchronizationManager.clear();
    }
}
