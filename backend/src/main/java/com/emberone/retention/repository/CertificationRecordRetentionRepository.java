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
public interface CertificationRecordRetentionRepository extends Repository<RetentionRun, Long> {

    @Query(
            value =
                    """
            SELECT r.id
            FROM cert_user_records r
            WHERE r.expiry_date IS NOT NULL
              AND r.expiry_date < CAST(:cutoff AT TIME ZONE 'UTC' AS date)
            """,
            nativeQuery = true)
    List<Long> findEvidenceRecordIdsExpiredBefore(@Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM cert_user_records WHERE id IN (:recordIds)", nativeQuery = true)
    int deleteRecordsByIds(@Param("recordIds") List<Long> recordIds);
}
