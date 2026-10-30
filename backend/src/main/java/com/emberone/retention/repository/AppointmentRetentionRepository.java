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
public interface AppointmentRetentionRepository extends Repository<RetentionRun, Long> {

    @Query(
            value =
                    """
            SELECT a.storage_key
            FROM doc_entity_attachments a
            WHERE a.entity_type = 'APPOINTMENT'
              AND EXISTS (
                SELECT 1 FROM appointments ap
                WHERE ap.tenant_id = a.tenant_id
                  AND ap.id = a.entity_id
                  AND ap.ends_at < :cutoff
              )
            """,
            nativeQuery = true)
    List<String> findAttachmentStorageKeysForAppointmentsEndingBefore(
            @Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM doc_entity_attachments a
            WHERE a.ctid IN (
                SELECT a2.ctid
                FROM doc_entity_attachments a2
                WHERE a2.entity_type = 'APPOINTMENT'
                  AND EXISTS (
                    SELECT 1 FROM appointments ap
                    WHERE ap.tenant_id = a2.tenant_id
                      AND ap.id = a2.entity_id
                      AND ap.ends_at < :cutoff
                  )
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteAttachmentsForAppointmentsEndingBefore(@Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM appointments
            WHERE ctid IN (
                SELECT ctid FROM appointments
                WHERE ends_at < :cutoff
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteAppointmentsEndingBefore(@Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM appointment_series s
            WHERE s.ctid IN (
                SELECT s2.ctid
                FROM appointment_series s2
                WHERE s2.series_end IS NOT NULL
                  AND s2.series_end < :cutoff
                  AND NOT EXISTS (
                    SELECT 1 FROM appointments a WHERE a.series_id = s2.id
                  )
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteEmptySeriesEndingBefore(@Param("cutoff") Instant cutoff);
}
