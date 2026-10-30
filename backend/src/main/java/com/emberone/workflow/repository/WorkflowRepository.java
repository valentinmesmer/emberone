/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.repository;

import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface WorkflowRepository
        extends JpaRepository<Workflow, Long>, JpaSpecificationExecutor<Workflow> {

    @EntityGraph(attributePaths = {"participants", "decisions", "inputChanges"})
    @Query(
            """
        SELECT w FROM Workflow w
        WHERE w.id IN :ids
        """)
    List<Workflow> findWithDetailsByIdIn(@Param("ids") Collection<Long> ids);

    @Query(
            """
        SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM Workflow w
        WHERE w.openedByUserId = :openedByUserId
          AND w.workflowTypeId = :workflowTypeId
          AND w.status IN (
              com.emberone.workflow.domain.WorkflowStatus.OPEN,
              com.emberone.workflow.domain.WorkflowStatus.AWAITING_INPUT_CHANGE
          )
        """)
    boolean existsActiveByOpenedByUserIdAndWorkflowTypeId(
            @Param("openedByUserId") Long openedByUserId,
            @Param("workflowTypeId") Long workflowTypeId);

    @Query(
            """
        SELECT w FROM Workflow w
        WHERE w.openedByUserId = :openedByUserId
          AND w.workflowTypeId = :workflowTypeId
          AND w.status IN (
              com.emberone.workflow.domain.WorkflowStatus.OPEN,
              com.emberone.workflow.domain.WorkflowStatus.AWAITING_INPUT_CHANGE
          )
        """)
    List<Workflow> findActiveByOpenedByUserIdAndWorkflowTypeId(
            @Param("openedByUserId") Long openedByUserId,
            @Param("workflowTypeId") Long workflowTypeId);

    @Query(
            """
        SELECT w FROM Workflow w
        WHERE w.openedByUserId IN :userIds
          AND w.workflowTypeId = :workflowTypeId
          AND w.status = com.emberone.workflow.domain.WorkflowStatus.OPEN
        """)
    List<Workflow> findOpenByOpenedByUserIdInAndWorkflowTypeId(
            @Param("userIds") Collection<Long> userIds,
            @Param("workflowTypeId") Long workflowTypeId);

    @EntityGraph(attributePaths = {"participants", "decisions", "inputChanges"})
    Optional<Workflow> findWithDetailsById(Long id);

    @Query(
            """
        SELECT w FROM Workflow w
        WHERE w.openedByUserId = :userId
        ORDER BY w.id DESC
        """)
    List<Workflow> findOpenedByUser(@Param("userId") Long userId);

    @Query(
            """
        SELECT DISTINCT w.workflowTypeId FROM Workflow w
        WHERE w.openedByUserId = :userId
        """)
    List<Long> findDistinctWorkflowTypeIdsByOpenedByUserId(@Param("userId") Long userId);

    @Query(
            """
        SELECT DISTINCT w.workflowTypeId FROM Workflow w
        JOIN w.participants p
        WHERE w.openedByUserId = :userId
          AND p.organizationId = :organizationId
        """)
    List<Long> findDistinctWorkflowTypeIdsByOpenedByUserIdInOrganization(
            @Param("userId") Long userId, @Param("organizationId") Long organizationId);

    @Query(
            """
        SELECT DISTINCT w.workflowTypeId FROM Workflow w
        JOIN w.participants p
        WHERE p.organizationId = :organizationId
        """)
    List<Long> findDistinctWorkflowTypeIdsForOrganization(
            @Param("organizationId") Long organizationId);

    @Query(
            """
        SELECT DISTINCT w.workflowTypeId FROM Workflow w
        JOIN w.participants p
        WHERE (
            (p.role = com.emberone.workflow.domain.ParticipantRole.VIEWER
                AND p.principalType = com.emberone.workflow.domain.PrincipalType.USER
                AND p.principalId = :userId)
            OR (p.role = com.emberone.workflow.domain.ParticipantRole.APPROVER
                AND p.principalType = com.emberone.workflow.domain.PrincipalType.USER
                AND p.principalId = :userId)
            OR (p.role = com.emberone.workflow.domain.ParticipantRole.APPROVER
                AND p.principalType = com.emberone.workflow.domain.PrincipalType.ORGANIZATION_GROUP
                AND p.principalId IN :groupIds)
        )
        """)
    List<Long> findDistinctWorkflowTypeIdsForParticipantCandidates(
            @Param("userId") Long userId, @Param("groupIds") List<Long> groupIds);

    @Query(
            """
        SELECT w FROM Workflow w
        WHERE w.openedByUserId = :userId
          AND w.status IN :statuses
        ORDER BY w.id DESC
        """)
    Page<Workflow> findOpenedByUser(
            @Param("userId") Long userId,
            @Param("statuses") Collection<WorkflowStatus> statuses,
            Pageable pageable);

    @Query(
            """
        SELECT w FROM Workflow w
        WHERE w.openedByUserId = :userId
          AND w.status IN (
              com.emberone.workflow.domain.WorkflowStatus.OPEN,
              com.emberone.workflow.domain.WorkflowStatus.AWAITING_INPUT_CHANGE
          )
        ORDER BY w.id DESC
        """)
    List<Workflow> findActiveByOpenedByUserId(@Param("userId") Long userId);

    /**
     * Open workflows where the user is a viewer or approver (direct user slot or organization-group
     * slot). Caller must exclude own opened workflows when self-approve is not allowed and limit to
     * actionable slots.
     */
    @Query(
            """
        SELECT DISTINCT w FROM Workflow w
        JOIN w.participants p
        WHERE w.status IN :statuses
          AND (
             (p.role = com.emberone.workflow.domain.ParticipantRole.VIEWER
                 AND p.principalType = com.emberone.workflow.domain.PrincipalType.USER
                 AND p.principalId = :userId)
             OR (p.role = com.emberone.workflow.domain.ParticipantRole.APPROVER
                 AND p.principalType = com.emberone.workflow.domain.PrincipalType.USER
                 AND p.principalId = :userId)
             OR (p.role = com.emberone.workflow.domain.ParticipantRole.APPROVER
                 AND p.principalType = com.emberone.workflow.domain.PrincipalType.ORGANIZATION_GROUP
                 AND p.principalId IN :userGroupIds)
          )
        ORDER BY w.id DESC
        """)
    Page<Workflow> findParticipantWorkflowsForUser(
            @Param("userId") Long userId,
            @Param("userGroupIds") List<Long> userGroupIds,
            @Param("statuses") Collection<WorkflowStatus> statuses,
            Pageable pageable);

    @Query(
            """
        SELECT DISTINCT w FROM Workflow w
        JOIN w.participants p
        WHERE w.status IN :statuses
          AND p.organizationId = :organizationId
        ORDER BY w.id DESC
        """)
    Page<Workflow> findOpenByOrganizationId(
            @Param("organizationId") Long organizationId,
            @Param("statuses") Collection<WorkflowStatus> statuses,
            Pageable pageable);

    @Query(
            """
        SELECT w FROM Workflow w
        WHERE w.workflowTypeId = :workflowTypeId
          AND w.status IN (
              com.emberone.workflow.domain.WorkflowStatus.OPEN,
              com.emberone.workflow.domain.WorkflowStatus.AWAITING_INPUT_CHANGE
          )
        """)
    List<Workflow> findOpenWithDetailsByWorkflowTypeId(
            @Param("workflowTypeId") Long workflowTypeId);

    @Query(
            """
        SELECT w FROM Workflow w
        WHERE w.openedByUserId = :userId
          AND w.workflowTypeId = :workflowTypeId
          AND w.subjectEntityType = :subjectEntityType
          AND w.subjectEntityId = :subjectEntityId
          AND w.status IN (
              com.emberone.workflow.domain.WorkflowStatus.OPEN,
              com.emberone.workflow.domain.WorkflowStatus.AWAITING_INPUT_CHANGE
          )
        """)
    Optional<Workflow> findActiveBySubject(
            @Param("userId") Long userId,
            @Param("workflowTypeId") Long workflowTypeId,
            @Param("subjectEntityType") String subjectEntityType,
            @Param("subjectEntityId") Long subjectEntityId);

    @Query(
            """
        SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM Workflow w
        WHERE w.workflowTypeId = :workflowTypeId
          AND w.subjectEntityType = :subjectEntityType
          AND w.subjectEntityId = :subjectEntityId
          AND w.status IN (
              com.emberone.workflow.domain.WorkflowStatus.OPEN,
              com.emberone.workflow.domain.WorkflowStatus.AWAITING_INPUT_CHANGE
          )
        """)
    boolean existsActiveByWorkflowTypeIdAndSubject(
            @Param("workflowTypeId") Long workflowTypeId,
            @Param("subjectEntityType") String subjectEntityType,
            @Param("subjectEntityId") Long subjectEntityId);

    @Query(
            """
        SELECT w FROM Workflow w
        WHERE w.subjectEntityType = :subjectEntityType
          AND w.subjectEntityId = :subjectEntityId
          AND w.status IN (
              com.emberone.workflow.domain.WorkflowStatus.OPEN,
              com.emberone.workflow.domain.WorkflowStatus.AWAITING_INPUT_CHANGE
          )
        """)
    List<Workflow> findActiveBySubjectEntity(
            @Param("subjectEntityType") String subjectEntityType,
            @Param("subjectEntityId") Long subjectEntityId);

    @Query(
            """
        SELECT w FROM Workflow w
        WHERE w.workflowTypeId IN :workflowTypeIds
          AND w.status IN (
              com.emberone.workflow.domain.WorkflowStatus.OPEN,
              com.emberone.workflow.domain.WorkflowStatus.AWAITING_INPUT_CHANGE
          )
        """)
    List<Workflow> findActiveByWorkflowTypeIdIn(
            @Param("workflowTypeIds") Collection<Long> workflowTypeIds);

    /** Stale-workflow candidates for retention ({@code WORKFLOW_STALE}), not locked. */
    @Query(
            value =
                    """
            SELECT w.id FROM wf_instances w
            WHERE w.opened_at < :cutoff
              AND w.status IN ('OPEN', 'AWAITING_INPUT_CHANGE')
            ORDER BY w.id
            """,
            nativeQuery = true)
    List<Long> findStaleOpenWorkflowIds(@Param("cutoff") Instant cutoff);

    /**
     * Locks one stale candidate with {@code SKIP LOCKED} and re-checks its status, so parallel
     * retention runs cancel each workflow at most once.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    @Query(
            value =
                    """
            SELECT * FROM wf_instances w
            WHERE w.id = :workflowId
              AND w.opened_at < :cutoff
              AND w.status IN ('OPEN', 'AWAITING_INPUT_CHANGE')
            FOR UPDATE SKIP LOCKED
            """,
            nativeQuery = true)
    Optional<Workflow> findAndLockStaleOpenWorkflow(
            @Param("workflowId") Long workflowId, @Param("cutoff") Instant cutoff);
}
