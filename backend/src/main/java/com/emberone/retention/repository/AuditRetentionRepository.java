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
public interface AuditRetentionRepository extends Repository<RetentionRun, Long> {

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM action_log
            WHERE ctid IN (
                SELECT ctid FROM action_log
                WHERE changed_at < :cutoff
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteEntriesChangedBefore(@Param("cutoff") Instant cutoff);
}
