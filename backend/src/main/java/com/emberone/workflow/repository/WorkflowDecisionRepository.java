/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.repository;

import com.emberone.workflow.domain.DecisionType;
import com.emberone.workflow.domain.WorkflowDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface WorkflowDecisionRepository extends JpaRepository<WorkflowDecision, Long> {

    boolean existsByWorkflow_IdAndUserId(Long workflowId, Long userId);

    boolean existsByWorkflow_IdAndParticipantIdAndUserIdAndDecisionType(
            Long workflowId, Long participantId, Long userId, DecisionType decisionType);
}
