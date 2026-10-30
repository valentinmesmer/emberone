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
public interface EventRetentionRepository extends Repository<RetentionRun, Long> {

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM cert_event_grant_recipients r
            WHERE r.ctid IN (
                SELECT r2.ctid
                FROM cert_event_grant_recipients r2
                INNER JOIN cert_event_grants g
                  ON r2.tenant_id = g.tenant_id AND r2.grant_id = g.id
                WHERE (
                  (g.event_type = 'SERVICE' AND EXISTS (
                    SELECT 1 FROM service_events e
                    WHERE e.tenant_id = g.tenant_id
                      AND e.id = g.event_id
                      AND e.starts_at < :cutoff
                  ))
                  OR (g.event_type = 'OPERATION' AND EXISTS (
                    SELECT 1 FROM operation_events e
                    WHERE e.tenant_id = g.tenant_id
                      AND e.id = g.event_id
                      AND e.starts_at < :cutoff
                  ))
                )
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteCertEventGrantRecipientsForEventsBefore(@Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM cert_event_grants g
            WHERE g.ctid IN (
                SELECT g2.ctid
                FROM cert_event_grants g2
                WHERE (g2.event_type = 'SERVICE' AND EXISTS (
                    SELECT 1 FROM service_events e
                    WHERE e.tenant_id = g2.tenant_id
                      AND e.id = g2.event_id
                      AND e.starts_at < :cutoff
                  ))
                  OR (g2.event_type = 'OPERATION' AND EXISTS (
                    SELECT 1 FROM operation_events e
                    WHERE e.tenant_id = g2.tenant_id
                      AND e.id = g2.event_id
                      AND e.starts_at < :cutoff
                  ))
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteCertEventGrantsForEventsBefore(@Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM event_attendances a
            WHERE a.ctid IN (
                SELECT a2.ctid
                FROM event_attendances a2
                WHERE (a2.event_type = 'SERVICE' AND EXISTS (
                    SELECT 1 FROM service_events e
                    WHERE e.tenant_id = a2.tenant_id
                      AND e.id = a2.event_id
                      AND e.starts_at < :cutoff
                  ))
                  OR (a2.event_type = 'OPERATION' AND EXISTS (
                    SELECT 1 FROM operation_events e
                    WHERE e.tenant_id = a2.tenant_id
                      AND e.id = a2.event_id
                      AND e.starts_at < :cutoff
                  ))
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteEventAttendancesForEventsBefore(@Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM service_events
            WHERE ctid IN (
                SELECT ctid FROM service_events
                WHERE starts_at < :cutoff
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteServiceEventsBefore(@Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM operation_events
            WHERE ctid IN (
                SELECT ctid FROM operation_events
                WHERE starts_at < :cutoff
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteOperationEventsBefore(@Param("cutoff") Instant cutoff);
}
