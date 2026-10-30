/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.storage.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.emberone.platform.storage.config.ObjectStorageProperties;
import com.emberone.platform.storage.domain.ObjectPendingDeletion;
import com.emberone.platform.storage.domain.ObjectPendingDeletionStatus;
import com.emberone.platform.storage.repository.ObjectPendingDeletionRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObjectStorageDeletionOutboxServiceTest {

    @Mock ObjectPendingDeletionRepository pendingDeletionRepository;
    @Mock ObjectStorageDeletionOutboxTransactionService transactionService;
    @Mock ObjectStorageService objectStorageService;

    ObjectStorageDeletionOutboxService outboxService;
    ObjectStorageProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ObjectStorageProperties();
        properties.setMaxDeletionAttempts(3);
        properties.setDeletionOutboxBatchSize(50);
        outboxService =
                new ObjectStorageDeletionOutboxService(
                        pendingDeletionRepository,
                        transactionService,
                        objectStorageService,
                        properties);
    }

    @Test
    void enqueueRejectsKeysOutsideTenantPrefix() {
        assertThatThrownBy(
                        () ->
                                outboxService.enqueueObjectDeletion(
                                        7L, "documents", "tenants/99/users/1/a.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenants/7/");
    }

    @Test
    void processPendingDeletionRemovesRowOnSuccess() {
        ObjectPendingDeletion entry =
                ObjectPendingDeletion.builder()
                        .id(1L)
                        .tenantId(7L)
                        .bucketName("documents")
                        .objectKey("tenants/7/users/1/a.pdf")
                        .status(ObjectPendingDeletionStatus.IN_PROGRESS)
                        .attempts(0)
                        .build();
        when(transactionService.claimPendingDeletionIds(any(), any(), anyInt()))
                .thenReturn(List.of(1L));
        when(transactionService.loadClaimedDeletion(eq(1L), eq(false))).thenReturn(entry);

        outboxService.processPendingDeletions();

        verify(objectStorageService).deleteFile("documents", "tenants/7/users/1/a.pdf");
        verify(transactionService).deleteCompleted(1L, false);
        verify(transactionService, never())
                .markFailed(anyLong(), anyInt(), any(), anyBoolean(), anyBoolean());
    }

    @Test
    void processPendingDeletionRetriesUntilMaxAttempts() {
        ObjectPendingDeletion entry =
                ObjectPendingDeletion.builder()
                        .id(2L)
                        .tenantId(7L)
                        .bucketName("documents")
                        .objectKey("tenants/7/users/1/b.pdf")
                        .status(ObjectPendingDeletionStatus.IN_PROGRESS)
                        .attempts(2)
                        .build();
        when(transactionService.claimPendingDeletionIds(any(), any(), anyInt()))
                .thenReturn(List.of(2L));
        when(transactionService.loadClaimedDeletion(eq(2L), eq(false))).thenReturn(entry);
        doThrow(new RuntimeException("s3 down"))
                .when(objectStorageService)
                .deleteFile("documents", "tenants/7/users/1/b.pdf");

        outboxService.processPendingDeletions();

        verify(transactionService).markFailed(2L, 3, "s3 down", false, false);
        verify(transactionService, never()).deleteCompleted(anyLong(), anyBoolean());
    }

    @Test
    void processesAtMostOneClaimBatchPerInvocation() {
        when(transactionService.claimPendingDeletionIds(any(), any(), anyInt()))
                .thenReturn(List.of(1L));
        when(transactionService.loadClaimedDeletion(eq(1L), eq(false))).thenReturn(null);

        outboxService.processPendingDeletions();

        verify(transactionService, times(1)).claimPendingDeletionIds(any(), any(), anyInt());
    }

    @Test
    void processPendingDeletionKeepsPendingWhenBelowAttemptLimit() {
        ObjectPendingDeletion entry =
                ObjectPendingDeletion.builder()
                        .id(3L)
                        .tenantId(7L)
                        .bucketName("documents")
                        .objectKey("tenants/7/users/1/c.pdf")
                        .status(ObjectPendingDeletionStatus.IN_PROGRESS)
                        .attempts(0)
                        .build();
        when(transactionService.claimPendingDeletionIds(any(), any(), anyInt()))
                .thenReturn(List.of(3L));
        when(transactionService.loadClaimedDeletion(eq(3L), eq(false))).thenReturn(entry);
        doThrow(new RuntimeException("temporary"))
                .when(objectStorageService)
                .deleteFile("documents", "tenants/7/users/1/c.pdf");

        outboxService.processPendingDeletions();

        verify(transactionService).markFailed(3L, 1, "temporary", true, false);
    }
}
