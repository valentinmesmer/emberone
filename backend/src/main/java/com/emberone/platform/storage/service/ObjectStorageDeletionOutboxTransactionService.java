/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.storage.service;

import com.emberone.platform.outbox.OutboxBackoff;
import com.emberone.platform.security.PostgresSessionContext;
import com.emberone.platform.storage.config.ObjectStorageProperties;
import com.emberone.platform.storage.domain.ObjectPendingDeletion;
import com.emberone.platform.storage.domain.ObjectPendingDeletionStatus;
import com.emberone.platform.storage.repository.ObjectPendingDeletionRepository;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ObjectStorageDeletionOutboxTransactionService {

    private final ObjectPendingDeletionRepository pendingDeletionRepository;
    private final ObjectStorageProperties properties;
    private final DataSource dataSource;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> claimPendingDeletionIds(Instant now, Instant staleBefore, int limit) {
        return pendingDeletionRepository.claimPendingDeletionIds(now, staleBefore, limit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> claimPendingDeletionIdsForDetachedTenants(
            Instant now, Instant staleBefore, int limit) {
        enableObjectStoragePlatformScope();
        return pendingDeletionRepository.claimPendingDeletionIds(now, staleBefore, limit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public ObjectPendingDeletion loadClaimedDeletion(Long deletionId, boolean platformScope) {
        if (platformScope) {
            enableObjectStoragePlatformScope();
        }
        return pendingDeletionRepository.findById(deletionId).orElse(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteCompleted(Long deletionId, boolean platformScope) {
        if (platformScope) {
            enableObjectStoragePlatformScope();
        }
        pendingDeletionRepository.deleteById(deletionId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(
            Long deletionId, int attempts, String error, boolean retryable, boolean platformScope) {
        if (platformScope) {
            enableObjectStoragePlatformScope();
        }
        ObjectPendingDeletion entry = pendingDeletionRepository.findById(deletionId).orElse(null);
        if (entry == null || entry.getStatus() != ObjectPendingDeletionStatus.IN_PROGRESS) {
            return;
        }
        entry.setAttempts(attempts);
        entry.setLastError(error);
        entry.setProcessingStartedAt(null);
        if (retryable) {
            entry.setStatus(ObjectPendingDeletionStatus.PENDING);
            entry.setNextAttemptAt(OutboxBackoff.nextAttemptAfter(attempts, retryBackoff()));
        } else {
            entry.setStatus(ObjectPendingDeletionStatus.FAILED);
            entry.setNextAttemptAt(null);
        }
        pendingDeletionRepository.save(entry);
    }

    private Duration retryBackoff() {
        return properties.getDeletionRetryBackoff();
    }

    private void enableObjectStoragePlatformScope() {
        try {
            Connection connection = DataSourceUtils.getConnection(dataSource);
            PostgresSessionContext.setObjectStoragePlatform(connection, true);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to enable object storage platform scope", ex);
        }
    }
}
