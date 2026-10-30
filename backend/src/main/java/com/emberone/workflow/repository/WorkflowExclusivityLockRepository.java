/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.repository;

import com.emberone.workflow.domain.WorkflowExclusivityLock;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface WorkflowExclusivityLockRepository
        extends JpaRepository<WorkflowExclusivityLock, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM WorkflowExclusivityLock l WHERE l.workflowId = :workflowId")
    void deleteByWorkflowId(@Param("workflowId") Long workflowId);

    List<WorkflowExclusivityLock> findByWorkflowId(Long workflowId);

    @Query(
            """
        SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END
        FROM WorkflowExclusivityLock l
        INNER JOIN Workflow w ON w.id = l.workflowId AND w.tenantId = l.tenantId
        WHERE l.lockKey = :lockKey
          AND w.status IN (
              com.emberone.workflow.domain.WorkflowStatus.OPEN,
              com.emberone.workflow.domain.WorkflowStatus.AWAITING_INPUT_CHANGE
          )
          AND (:excludeWorkflowId IS NULL OR w.id <> :excludeWorkflowId)
        """)
    boolean existsActiveConflict(
            @Param("lockKey") String lockKey, @Param("excludeWorkflowId") Long excludeWorkflowId);
}
