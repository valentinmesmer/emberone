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
public interface WorkflowRetentionRepository extends Repository<RetentionRun, Long> {

    @Query(
            value =
                    """
            SELECT id FROM wf_instances
            WHERE decided_at IS NOT NULL
              AND decided_at < :cutoff
            """,
            nativeQuery = true)
    List<Long> findWorkflowIdsDecidedBefore(@Param("cutoff") Instant cutoff);

    @Query(
            value =
                    """
            SELECT a.storage_key
            FROM doc_entity_attachments a
            WHERE a.entity_type = 'WORKFLOW'
              AND EXISTS (
                SELECT 1 FROM wf_instances w
                WHERE w.tenant_id = a.tenant_id
                  AND w.id = a.entity_id
                  AND w.decided_at IS NOT NULL
                  AND w.decided_at < :cutoff
              )
            UNION
            SELECT a.storage_key
            FROM doc_entity_attachments a
            WHERE a.entity_type = 'WORKFLOW_COMMENT'
              AND EXISTS (
                SELECT 1 FROM wf_comments c
                INNER JOIN wf_instances w
                  ON c.tenant_id = w.tenant_id AND c.workflow_id = w.id
                WHERE c.tenant_id = a.tenant_id
                  AND c.id = a.entity_id
                  AND w.decided_at IS NOT NULL
                  AND w.decided_at < :cutoff
              )
            """,
            nativeQuery = true)
    List<String> findAttachmentStorageKeysForWorkflowsDecidedBefore(
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
                WHERE a2.entity_type = 'WORKFLOW'
                  AND EXISTS (
                    SELECT 1 FROM wf_instances w
                    WHERE w.tenant_id = a2.tenant_id
                      AND w.id = a2.entity_id
                      AND w.decided_at IS NOT NULL
                      AND w.decided_at < :cutoff
                  )
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteWorkflowAttachmentsForWorkflowsDecidedBefore(@Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM doc_entity_attachments a
            WHERE a.ctid IN (
                SELECT a2.ctid
                FROM doc_entity_attachments a2
                WHERE a2.entity_type = 'WORKFLOW_COMMENT'
                  AND EXISTS (
                    SELECT 1 FROM wf_comments c
                    INNER JOIN wf_instances w
                      ON c.tenant_id = w.tenant_id AND c.workflow_id = w.id
                    WHERE c.tenant_id = a2.tenant_id
                      AND c.id = a2.entity_id
                      AND w.decided_at IS NOT NULL
                      AND w.decided_at < :cutoff
                  )
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteWorkflowCommentAttachmentsForWorkflowsDecidedBefore(@Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value =
                    """
            DELETE FROM wf_instances
            WHERE ctid IN (
                SELECT ctid FROM wf_instances
                WHERE decided_at IS NOT NULL
                  AND decided_at < :cutoff
                LIMIT 5000
            )
            """,
            nativeQuery = true)
    int deleteWorkflowsDecidedBefore(@Param("cutoff") Instant cutoff);
}
