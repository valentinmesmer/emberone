/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.repository;

import com.emberone.todo.domain.Todo;
import com.emberone.todo.domain.TodoSourceEntityType;
import com.emberone.todo.domain.TodoType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface TodoRepository extends JpaRepository<Todo, Long>, JpaSpecificationExecutor<Todo> {

    /**
     * Loads a todo with {@code assigneeGroups} and {@code excludedUsers} initialized (both are
     * bags, so they cannot be join-fetched together). For callers outside a transaction, e.g.
     * after-commit realtime callbacks.
     */
    default Optional<Todo> findByIdWithRecipients(Long id) {
        return findById(id)
                .map(
                        todo -> {
                            Hibernate.initialize(todo.getAssigneeGroups());
                            Hibernate.initialize(todo.getExcludedUsers());
                            return todo;
                        });
    }

    @Query(
            """
        SELECT DISTINCT t FROM Todo t
        LEFT JOIN FETCH t.assigneeGroups
        WHERE t.id IN :ids
        """)
    List<Todo> findWithAssigneeGroupsByIdIn(@Param("ids") Collection<Long> ids);

    @Query(
            """
        SELECT COUNT(DISTINCT t) FROM Todo t
        WHERE t.archivedAt IS NULL
        AND t.status = com.emberone.todo.domain.TodoStatus.OPEN
        AND NOT EXISTS (
            SELECT 1 FROM TodoExcludedUser e
            WHERE e.todo = t AND e.userId = :userId
        )
        AND (
            t.assigneeUserId = :userId
            OR EXISTS (
                SELECT 1 FROM TodoAssigneeGroup g
                WHERE g.todo = t
                AND g.organizationGroupId IN :groupIds
            )
        )
        """)
    long countOpenVisibleForUser(
            @Param("userId") Long userId, @Param("groupIds") List<Long> groupIds);

    @Query(
            """
        SELECT DISTINCT t.type FROM Todo t
        WHERE NOT EXISTS (
            SELECT 1 FROM TodoExcludedUser e
            WHERE e.todo = t AND e.userId = :userId
        )
        AND (
            t.assigneeUserId = :userId
            OR EXISTS (
                SELECT 1 FROM TodoAssigneeGroup g
                WHERE g.todo = t
                AND g.organizationGroupId IN :groupIds
            )
        )
        """)
    List<TodoType> findDistinctTypesVisibleToUser(
            @Param("userId") Long userId, @Param("groupIds") List<Long> groupIds);

    @Query(
            """
        SELECT t FROM Todo t
        LEFT JOIN FETCH t.assigneeGroups
        WHERE t.id = :id
        AND NOT EXISTS (
            SELECT 1 FROM TodoExcludedUser e
            WHERE e.todo = t AND e.userId = :userId
        )
        AND (
            t.assigneeUserId = :userId
            OR EXISTS (
                SELECT 1 FROM TodoAssigneeGroup g
                WHERE g.todo = t
                AND g.organizationGroupId IN :groupIds
            )
        )
        """)
    Optional<Todo> findVisibleByIdForUser(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("groupIds") List<Long> groupIds);

    @Query(
            """
        SELECT t FROM Todo t
        WHERE t.sourceEntityType = :sourceType
        AND t.sourceEntityId = :sourceId
        AND t.status = com.emberone.todo.domain.TodoStatus.OPEN
        AND t.archivedAt IS NULL
        """)
    List<Todo> findOpenBySource(
            @Param("sourceType") TodoSourceEntityType sourceType, @Param("sourceId") Long sourceId);

    @Query(
            value =
                    """
        SELECT t.* FROM todo_items t
        WHERE t.source_entity_type = :sourceType
        AND t.source_entity_id = :sourceId
        AND t.type = :type
        AND t.status = 'OPEN'
        AND t.archived_at IS NULL
        AND (t.params->>'participantId')::bigint = :participantId
        LIMIT 1
        """,
            nativeQuery = true)
    Optional<Todo> findOpenBySourceTypeAndParticipant(
            @Param("sourceType") String sourceType,
            @Param("sourceId") Long sourceId,
            @Param("type") String type,
            @Param("participantId") Long participantId);

    @Query(
            value =
                    """
        SELECT t.* FROM todo_items t
        WHERE t.source_entity_type = :sourceType
        AND t.source_entity_id = :sourceId
        AND t.type = :type
        AND (t.params->>'participantId')::bigint = :participantId
        ORDER BY t.id DESC
        LIMIT 1
        """,
            nativeQuery = true)
    Optional<Todo> findLatestBySourceTypeAndParticipant(
            @Param("sourceType") String sourceType,
            @Param("sourceId") Long sourceId,
            @Param("type") String type,
            @Param("participantId") Long participantId);

    @Query(
            """
        SELECT t FROM Todo t
        WHERE t.sourceEntityType = :sourceType
        AND t.sourceEntityId = :sourceId
        AND t.type = :type
        AND t.status = com.emberone.todo.domain.TodoStatus.OPEN
        AND t.archivedAt IS NULL
        AND t.assigneeUserId = :assigneeUserId
        """)
    List<Todo> findOpenBySourceTypeAndAssignee(
            @Param("sourceType") TodoSourceEntityType sourceType,
            @Param("sourceId") Long sourceId,
            @Param("type") TodoType type,
            @Param("assigneeUserId") Long assigneeUserId);

    @Query(
            """
        SELECT DISTINCT t FROM Todo t
        LEFT JOIN FETCH t.assigneeGroups
        WHERE t.archivedAt IS NULL
        AND t.status = com.emberone.todo.domain.TodoStatus.OPEN
        AND EXISTS (
            SELECT 1 FROM TodoAssigneeGroup g
            WHERE g.todo = t
            AND g.organizationGroupId = :organizationGroupId
        )
        """)
    List<Todo> findOpenByOrganizationGroupId(
            @Param("organizationGroupId") Long organizationGroupId);

    @Query(
            """
        SELECT t FROM Todo t
        WHERE t.assigneeUserId = :assigneeUserId
        AND t.type = :type
        AND t.sourceEntityType = :sourceType
        AND t.status = com.emberone.todo.domain.TodoStatus.OPEN
        AND t.archivedAt IS NULL
        """)
    List<Todo> findOpenByAssigneeTypeAndSourceType(
            @Param("assigneeUserId") Long assigneeUserId,
            @Param("type") TodoType type,
            @Param("sourceType") TodoSourceEntityType sourceType);
}
