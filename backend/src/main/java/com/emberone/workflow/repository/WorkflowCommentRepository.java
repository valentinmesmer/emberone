/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.repository;

import com.emberone.workflow.domain.WorkflowComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface WorkflowCommentRepository extends JpaRepository<WorkflowComment, Long> {

    List<WorkflowComment> findByWorkflow_IdOrderByIdAsc(Long workflowId);
}
