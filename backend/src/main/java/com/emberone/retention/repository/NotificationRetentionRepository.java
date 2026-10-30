/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.repository;

import com.emberone.retention.domain.RetentionRun;
import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface NotificationRetentionRepository extends Repository<RetentionRun, Long> {

    /**
     * Unread ({@code read_at IS NULL}): delete when {@code created_at} is before {@code cutoff}.
     * Read ({@code read_at IS NOT NULL}): delete on every retention run.
     */
    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM ntf_notifications
            WHERE ctid IN (
                SELECT ctid FROM ntf_notifications
                WHERE read_at IS NOT NULL
                   OR created_at < :cutoff
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteNotificationsEligibleForRetention(@Param("cutoff") Instant cutoff);
}
