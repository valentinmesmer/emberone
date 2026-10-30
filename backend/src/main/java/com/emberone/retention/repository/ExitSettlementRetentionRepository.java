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
public interface ExitSettlementRetentionRepository extends Repository<RetentionRun, Long> {

    @Query(
            value =
                    """
            SELECT d.storage_key
            FROM doc_documents d
            INNER JOIN users u
              ON d.tenant_id = u.tenant_id AND d.user_id = u.id
            WHERE u.deactivated_at IS NOT NULL
              AND u.deactivated_at < :cutoff
              AND NOT EXISTS (
                SELECT 1 FROM doc_document_links l
                WHERE l.tenant_id = d.tenant_id AND l.document_id = d.id
              )
            """,
            nativeQuery = true)
    List<String> findUnlinkedDocumentStorageKeysForDeactivatedUsersBefore(
            @Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM doc_documents d
            WHERE d.ctid IN (
                SELECT d2.ctid
                FROM doc_documents d2
                INNER JOIN users u
                  ON d2.tenant_id = u.tenant_id AND d2.user_id = u.id
                WHERE u.deactivated_at IS NOT NULL
                  AND u.deactivated_at < :cutoff
                  AND NOT EXISTS (
                    SELECT 1 FROM doc_document_links l
                    WHERE l.tenant_id = d2.tenant_id AND l.document_id = d2.id
                  )
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteUnlinkedDocumentsForDeactivatedUsersBefore(@Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM user_contact_information c
            WHERE c.ctid IN (
                SELECT c2.ctid
                FROM user_contact_information c2
                INNER JOIN users u
                  ON c2.tenant_id = u.tenant_id AND c2.user_id = u.id
                WHERE u.deactivated_at IS NOT NULL
                  AND u.deactivated_at < :cutoff
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteContactInformationForDeactivatedUsersBefore(@Param("cutoff") Instant cutoff);
}
