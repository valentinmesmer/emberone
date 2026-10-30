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
public interface DrivingLicenseRetentionRepository extends Repository<RetentionRun, Long> {

    @Query(
            value =
                    """
            SELECT dl.id
            FROM driving_licenses dl
            INNER JOIN users u
              ON dl.tenant_id = u.tenant_id AND dl.user_id = u.id
            WHERE u.deactivated_at IS NOT NULL
              AND u.deactivated_at < :cutoff
            """,
            nativeQuery = true)
    List<Long> findDrivingLicenseIdsForDeactivatedUsersBefore(@Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM driving_licenses dl
            WHERE dl.ctid IN (
                SELECT dl2.ctid
                FROM driving_licenses dl2
                INNER JOIN users u
                  ON dl2.tenant_id = u.tenant_id AND dl2.user_id = u.id
                WHERE u.deactivated_at IS NOT NULL
                  AND u.deactivated_at < :cutoff
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteDrivingLicensesForDeactivatedUsersBefore(@Param("cutoff") Instant cutoff);
}
