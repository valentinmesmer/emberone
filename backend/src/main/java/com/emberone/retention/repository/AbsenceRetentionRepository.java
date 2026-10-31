/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
public interface AbsenceRetentionRepository extends Repository<RetentionRun, Long> {

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM user_absences
            WHERE ctid IN (
                SELECT ctid FROM user_absences
                WHERE end_date < CAST(:cutoff AT TIME ZONE 'UTC' AS date)
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteAbsencesEndingBefore(@Param("cutoff") Instant cutoff);
}
