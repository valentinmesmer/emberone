/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.repository;

import com.emberone.retention.domain.RetentionRun;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface UserMasterRetentionRepository extends Repository<RetentionRun, Long> {

    @Query(
            value =
                    """
            SELECT u.id
            FROM users u
            WHERE u.deactivated_at IS NOT NULL
              AND u.deactivated_at < :cutoff
            """,
            nativeQuery = true)
    List<Long> findDeactivatedUserIdsBefore(@Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM users u
            WHERE u.ctid IN (
                SELECT u2.ctid
                FROM users u2
                WHERE u2.deactivated_at IS NOT NULL
                  AND u2.deactivated_at < :cutoff
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteDeactivatedUsersBefore(@Param("cutoff") Instant cutoff);
}
