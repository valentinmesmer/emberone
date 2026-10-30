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
public interface ExitImmediateRetentionRepository extends Repository<RetentionRun, Long> {

    @Query(
            value =
                    """
            SELECT pi.profile_image_url
            FROM doc_user_profile_images pi
            INNER JOIN users u
              ON pi.tenant_id = u.tenant_id AND pi.user_id = u.id
            WHERE u.deactivated_at IS NOT NULL
              AND u.deactivated_at < :cutoff
              AND pi.profile_image_url IS NOT NULL
              AND pi.profile_image_url <> ''
            """,
            nativeQuery = true)
    List<String> findProfileImageObjectKeysForDeactivatedUsersBefore(
            @Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM doc_user_profile_images pi
            WHERE pi.ctid IN (
                SELECT pi2.ctid
                FROM doc_user_profile_images pi2
                INNER JOIN users u
                  ON pi2.tenant_id = u.tenant_id AND pi2.user_id = u.id
                WHERE u.deactivated_at IS NOT NULL
                  AND u.deactivated_at < :cutoff
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteProfileImagesForDeactivatedUsersBefore(@Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM user_emergency_contacts ec
            WHERE ec.ctid IN (
                SELECT ec2.ctid
                FROM user_emergency_contacts ec2
                INNER JOIN users u
                  ON ec2.tenant_id = u.tenant_id AND ec2.user_id = u.id
                WHERE u.deactivated_at IS NOT NULL
                  AND u.deactivated_at < :cutoff
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteEmergencyContactsForDeactivatedUsersBefore(@Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM ntf_user_channel_overrides o
            WHERE o.ctid IN (
                SELECT o2.ctid
                FROM ntf_user_channel_overrides o2
                INNER JOIN users u
                  ON o2.tenant_id = u.tenant_id AND o2.user_id = u.id
                WHERE u.deactivated_at IS NOT NULL
                  AND u.deactivated_at < :cutoff
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteChannelOverridesForDeactivatedUsersBefore(@Param("cutoff") Instant cutoff);
}
