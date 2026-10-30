/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.repository;

import com.emberone.workflow.domain.WorkflowParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface WorkflowParticipantRepository extends JpaRepository<WorkflowParticipant, Long> {

    Optional<WorkflowParticipant> findByIdAndWorkflow_Id(Long id, Long workflowId);

    /**
     * Open workflow slots claimed by a user for a specific organization-group approver principal.
     */
    @Query(
            """
        SELECT p FROM WorkflowParticipant p
        JOIN FETCH p.workflow w
        WHERE p.claimedByUserId = :userId
          AND p.principalType = com.emberone.workflow.domain.PrincipalType.ORGANIZATION_GROUP
          AND p.principalId = :organizationGroupId
          AND p.slotStatus = com.emberone.workflow.domain.ParticipantSlotStatus.PENDING
          AND w.status IN (
              com.emberone.workflow.domain.WorkflowStatus.OPEN,
              com.emberone.workflow.domain.WorkflowStatus.AWAITING_INPUT_CHANGE
          )
        """)
    List<WorkflowParticipant> findOpenPendingClaimsByUserAndOrganizationGroup(
            @Param("userId") Long userId, @Param("organizationGroupId") Long organizationGroupId);

    /** All open workflow slots claimed by a user, regardless of approver principal. */
    @Query(
            """
        SELECT p FROM WorkflowParticipant p
        JOIN FETCH p.workflow w
        WHERE p.claimedByUserId = :userId
          AND p.slotStatus = com.emberone.workflow.domain.ParticipantSlotStatus.PENDING
          AND w.status IN (
              com.emberone.workflow.domain.WorkflowStatus.OPEN,
              com.emberone.workflow.domain.WorkflowStatus.AWAITING_INPUT_CHANGE
          )
        """)
    List<WorkflowParticipant> findOpenPendingClaimsByUser(@Param("userId") Long userId);
}
