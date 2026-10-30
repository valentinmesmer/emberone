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
public interface OnboardingRetentionRepository extends Repository<RetentionRun, Long> {

    @Query(
            value =
                    """
            SELECT i.id FROM onb_invitations i
            WHERE i.status = 'APPROVED'
              AND EXISTS (
                SELECT 1 FROM users u
                WHERE u.tenant_id = i.tenant_id
                  AND u.id = i.user_id
                  AND u.deactivated_at IS NOT NULL
                  AND u.deactivated_at < :cutoff
              )
            """,
            nativeQuery = true)
    List<Long> findApprovedInvitationIdsCompletedBefore(@Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM onb_invitations i
            WHERE i.ctid IN (
                SELECT i2.ctid
                FROM onb_invitations i2
                WHERE i2.status = 'APPROVED'
                  AND EXISTS (
                    SELECT 1 FROM users u
                    WHERE u.tenant_id = i2.tenant_id
                      AND u.id = i2.user_id
                      AND u.deactivated_at IS NOT NULL
                      AND u.deactivated_at < :cutoff
                  )
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteApprovedInvitationsCompletedBefore(@Param("cutoff") Instant cutoff);
}
