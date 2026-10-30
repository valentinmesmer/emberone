/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.repository;

import com.emberone.identity.domain.IdentityUser;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface IdentityUserRepository
        extends JpaRepository<IdentityUser, Long>, JpaSpecificationExecutor<IdentityUser> {
    Optional<IdentityUser> findByEmberoneUserId(Long emberoneUserId);

    @Transactional
    @Query(
            value =
                    """
            WITH picked AS (
                SELECT s.id FROM idm_users s
                WHERE s.deprovision_pending_at IS NOT NULL
                  AND s.deprovision_attempts < :maxAttempts
                  AND (
                    (s.deprovision_processing_started_at IS NULL
                        AND (s.deprovision_next_attempt_at IS NULL
                            OR s.deprovision_next_attempt_at <= :now))
                    OR (s.deprovision_processing_started_at IS NOT NULL
                        AND s.deprovision_processing_started_at < :staleBefore)
                  )
                ORDER BY s.deprovision_pending_at, s.id
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            UPDATE idm_users u
            SET deprovision_processing_started_at = :now
            FROM picked
            WHERE u.id = picked.id
            RETURNING u.id
            """,
            nativeQuery = true)
    List<Long> claimPendingDeprovisionIds(
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            @Param("limit") int limit,
            @Param("maxAttempts") int maxAttempts);

    // Explicit tenant filter: tenant purge path; idm_users also has the cross-tenant
    // global-admin read policy.
    List<IdentityUser> findAllByTenantIdOrderByIdAsc(Long tenantId);
}
