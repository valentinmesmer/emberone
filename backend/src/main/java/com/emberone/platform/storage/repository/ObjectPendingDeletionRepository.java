/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.storage.repository;

import com.emberone.platform.storage.domain.ObjectPendingDeletion;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface ObjectPendingDeletionRepository
        extends JpaRepository<ObjectPendingDeletion, Long> {

    @Transactional
    @Query(
            value =
                    """
            WITH picked AS (
                SELECT s.id FROM obj_pending_deletions s
                WHERE (
                    (s.status = 'PENDING'
                        AND (s.next_attempt_at IS NULL OR s.next_attempt_at <= :now))
                    OR (s.status = 'IN_PROGRESS' AND s.processing_started_at < :staleBefore)
                )
                ORDER BY s.id
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            UPDATE obj_pending_deletions d
            SET status = 'IN_PROGRESS', processing_started_at = :now
            FROM picked
            WHERE d.id = picked.id
            RETURNING d.id
            """,
            nativeQuery = true)
    List<Long> claimPendingDeletionIds(
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            @Param("limit") int limit);
}
