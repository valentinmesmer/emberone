/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.repository;

import com.emberone.notification.domain.NotificationDelivery;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    @Transactional
    @Query(
            value =
                    """
            WITH picked AS (
                SELECT s.id FROM ntf_deliveries s
                WHERE (
                    (s.status = 'PENDING'
                        AND (s.next_attempt_at IS NULL OR s.next_attempt_at <= :now))
                    OR (s.status = 'IN_PROGRESS' AND s.processing_started_at < :staleBefore)
                )
                ORDER BY s.id
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            UPDATE ntf_deliveries d
            SET status = 'IN_PROGRESS', processing_started_at = :now
            FROM picked
            WHERE d.id = picked.id
            RETURNING d.id
            """,
            nativeQuery = true)
    List<Long> claimPendingDeliveryIds(
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            @Param("limit") int limit);

    @Query(
            """
        SELECT d FROM NotificationDelivery d
        JOIN FETCH d.notification
        WHERE d.id IN :ids
        ORDER BY d.id ASC
        """)
    List<NotificationDelivery> findByIdInWithNotification(@Param("ids") Collection<Long> ids);
}
