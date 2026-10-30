/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.service;

import com.emberone.platform.navigation.AppLinkSupport;
import com.emberone.platform.navigation.domain.AppLink;
import com.emberone.todo.domain.*;
import com.emberone.todo.event.TodoAssigneeGroupsChangedEvent;
import com.emberone.todo.event.TodoCreatedEvent;
import com.emberone.todo.event.TodoUpdatedEvent;
import com.emberone.todo.exception.TodoNotFoundException;
import com.emberone.todo.exception.TodoStateException;
import com.emberone.todo.repository.TodoRepository;
import com.emberone.todo.type.TodoTypeDefinitionRegistry;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TodoTypeDefinitionRegistry typeDefinitions;

    private static Long participantId(java.util.Map<String, Object> params) {
        if (params == null) {
            return null;
        }
        Object value = params.get(TodoParamKeys.PARTICIPANT_ID);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private static void validateOpenRequest(TodoOpenRequest request) {
        if (request == null || request.tenantId() == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (request.type() == null) {
            throw new IllegalArgumentException("type is required");
        }
        boolean hasUser = request.assigneeUserId() != null;
        boolean hasGroups = !CollectionUtils.isEmpty(request.organizationGroupIds());
        if (!hasUser && !hasGroups) {
            throw new IllegalArgumentException(
                    "At least one assignee (user or organization group) is required");
        }
    }

    private static void applyLink(Todo todo, AppLink link) {
        AppLinkSupport.applyTo(todo, link);
    }

    private static Set<Long> organizationGroupIdsOf(Todo todo) {
        Set<Long> groupIds = new LinkedHashSet<>();
        if (todo.getAssigneeGroups() == null) {
            return groupIds;
        }
        for (TodoAssigneeGroup group : todo.getAssigneeGroups()) {
            if (group.getOrganizationGroupId() != null) {
                groupIds.add(group.getOrganizationGroupId());
            }
        }
        return groupIds;
    }

    private static void replaceExcludedUsers(Todo todo, Set<Long> excludedUserIds) {
        todo.getExcludedUsers().clear();
        if (CollectionUtils.isEmpty(excludedUserIds)) {
            return;
        }
        for (Long userId : excludedUserIds) {
            if (userId == null) {
                continue;
            }
            todo.getExcludedUsers()
                    .add(
                            TodoExcludedUser.builder()
                                    .tenantId(todo.getTenantId())
                                    .todoId(todo.getId())
                                    .userId(userId)
                                    .todo(todo)
                                    .build());
        }
    }

    @Transactional
    public Todo open(TodoOpenRequest request) {
        validateOpenRequest(request);

        Optional<Todo> existing =
                findOpenBySourceParticipant(
                        request.sourceEntityType(),
                        request.sourceEntityId(),
                        request.type(),
                        participantId(request.params()));
        if (existing.isPresent()) {
            return existing.get();
        }

        Todo todo =
                Todo.builder()
                        .tenantId(request.tenantId())
                        .assigneeUserId(request.assigneeUserId())
                        .type(request.type())
                        .params(request.params())
                        .status(TodoStatus.OPEN)
                        .sourceEntityType(request.sourceEntityType())
                        .sourceEntityId(request.sourceEntityId())
                        .build();
        applyLink(todo, request.link());

        Todo saved = todoRepository.save(todo);

        if (!CollectionUtils.isEmpty(request.organizationGroupIds())) {
            for (Long groupId : request.organizationGroupIds()) {
                if (groupId == null) {
                    continue;
                }
                saved.getAssigneeGroups()
                        .add(
                                TodoAssigneeGroup.builder()
                                        .tenantId(request.tenantId())
                                        .todoId(saved.getId())
                                        .organizationGroupId(groupId)
                                        .todo(saved)
                                        .build());
            }
            saved = todoRepository.save(saved);
        }
        replaceExcludedUsers(saved, request.excludedUserIds());
        if (!CollectionUtils.isEmpty(request.excludedUserIds())) {
            saved = todoRepository.save(saved);
        }

        eventPublisher.publishEvent(
                new TodoCreatedEvent(saved.getId(), request.suppressCreateNotification()));
        return saved;
    }

    @Transactional
    public void completeBySource(
            TodoSourceEntityType sourceType, Long sourceId, Long completedByUserId) {
        finalizeBySource(sourceType, sourceId, TodoStatus.COMPLETED, completedByUserId);
    }

    @Transactional
    public void cancelBySource(
            TodoSourceEntityType sourceType, Long sourceId, Long completedByUserId) {
        finalizeBySource(sourceType, sourceId, TodoStatus.CANCELLED, completedByUserId);
    }

    @Transactional
    public void archiveOpenBySourceParticipant(
            TodoSourceEntityType sourceType,
            Long sourceId,
            TodoType type,
            Long participantId,
            Long completedByUserId) {
        findOpenBySourceParticipant(sourceType, sourceId, type, participantId)
                .ifPresent(todo -> archive(todo, TodoStatus.COMPLETED, completedByUserId));
    }

    @Transactional
    public void cancelOpenBySourceParticipant(
            TodoSourceEntityType sourceType,
            Long sourceId,
            TodoType type,
            Long participantId,
            Long completedByUserId) {
        findOpenBySourceParticipant(sourceType, sourceId, type, participantId)
                .ifPresent(todo -> archive(todo, TodoStatus.CANCELLED, completedByUserId));
    }

    /** Cancels every open todo tied to a workflow participant (claim and approval todos). */
    @Transactional
    public void cancelOpenTodosForWorkflowParticipant(
            TodoSourceEntityType sourceType,
            Long sourceId,
            Long participantId,
            Long completedByUserId) {
        if (sourceType == null || sourceId == null || participantId == null) {
            return;
        }
        for (Todo todo : todoRepository.findOpenBySource(sourceType, sourceId)) {
            if (participantId.equals(participantId(todo.getParams()))) {
                archive(todo, TodoStatus.CANCELLED, completedByUserId);
            }
        }
    }

    @Transactional
    public void reopenGroupClaimTodo(TodoOpenRequest request) {
        validateOpenRequest(request);
        Long participantId = participantId(request.params());
        Optional<Todo> open =
                findOpenBySourceParticipant(
                        request.sourceEntityType(),
                        request.sourceEntityId(),
                        TodoType.WORKFLOW_START_REQUESTED,
                        participantId);
        if (open.isPresent()) {
            syncOpenTodoFromRequest(open.get(), request);
            return;
        }
        Optional<Todo> latest =
                todoRepository.findLatestBySourceTypeAndParticipant(
                        request.sourceEntityType().name(),
                        request.sourceEntityId(),
                        TodoType.WORKFLOW_START_REQUESTED.name(),
                        participantId);
        if (latest.isPresent() && latest.get().getStatus() != TodoStatus.OPEN) {
            Todo restored = latest.get();
            applyOpenRequestToTodo(restored, request);
            reopen(restored, request.suppressCreateNotification());
            return;
        }
        if (latest.isEmpty()) {
            open(request);
        }
    }

    @Transactional
    public void reopenApprovalTodo(TodoOpenRequest request) {
        validateOpenRequest(request);
        Long participantId = participantId(request.params());
        Long assigneeUserId = request.assigneeUserId();
        Optional<Todo> open =
                findOpenBySourceParticipant(
                        request.sourceEntityType(),
                        request.sourceEntityId(),
                        TodoType.WORKFLOW_APPROVAL_REQUIRED,
                        participantId);
        if (open.isPresent()) {
            if (!assigneeUserId.equals(open.get().getAssigneeUserId())) {
                archive(open.get(), TodoStatus.CANCELLED, null);
                open(request);
                return;
            }
            syncOpenTodoFromRequest(open.get(), request);
            return;
        }
        Optional<Todo> latest =
                todoRepository.findLatestBySourceTypeAndParticipant(
                        request.sourceEntityType().name(),
                        request.sourceEntityId(),
                        TodoType.WORKFLOW_APPROVAL_REQUIRED.name(),
                        participantId);
        if (latest.isPresent()
                && assigneeUserId.equals(latest.get().getAssigneeUserId())
                && latest.get().getStatus() != TodoStatus.OPEN) {
            Todo restored = latest.get();
            applyOpenRequestToTodo(restored, request);
            reopen(restored, request.suppressCreateNotification());
            return;
        }
        open(request);
    }

    @Transactional
    public Todo completeManually(Long todoId, Long userId) {
        Todo todo =
                todoRepository
                        .findById(todoId)
                        .orElseThrow(() -> new TodoNotFoundException(todoId));
        if (!todo.isOpen()) {
            throw new TodoStateException("Todo is not open");
        }
        if (!typeDefinitions.require(todo.getType()).allowsManualComplete()) {
            throw new TodoStateException("Todo type does not allow manual completion");
        }
        return archive(todo, TodoStatus.COMPLETED, userId);
    }

    private void finalizeBySource(
            TodoSourceEntityType sourceType,
            Long sourceId,
            TodoStatus terminalStatus,
            Long completedByUserId) {
        List<Todo> openTodos = todoRepository.findOpenBySource(sourceType, sourceId);
        for (Todo todo : openTodos) {
            archive(todo, terminalStatus, completedByUserId);
        }
    }

    private Todo archive(Todo todo, TodoStatus terminalStatus, Long completedByUserId) {
        if (!todo.isOpen()) {
            return todo;
        }
        Instant now = Instant.now();
        todo.setStatus(terminalStatus);
        todo.setCompletedAt(now);
        todo.setCompletedByUserId(completedByUserId);
        todo.setArchivedAt(now);
        Todo saved = todoRepository.save(todo);
        eventPublisher.publishEvent(new TodoUpdatedEvent(saved.getId()));
        return saved;
    }

    private void syncOpenTodoFromRequest(Todo todo, TodoOpenRequest request) {
        Set<Long> previousGroupIds = organizationGroupIdsOf(todo);
        applyOpenRequestToTodo(todo, request);
        Todo saved = todoRepository.save(todo);
        eventPublisher.publishEvent(new TodoUpdatedEvent(saved.getId()));
        Set<Long> newGroupIds = organizationGroupIdsOf(saved);
        if (!previousGroupIds.equals(newGroupIds)) {
            eventPublisher.publishEvent(
                    new TodoAssigneeGroupsChangedEvent(
                            saved.getId(), previousGroupIds, newGroupIds));
        }
    }

    private void applyOpenRequestToTodo(Todo todo, TodoOpenRequest request) {
        todo.setParams(request.params() == null ? Map.of() : new HashMap<>(request.params()));
        applyLink(todo, request.link());
        if (!CollectionUtils.isEmpty(request.organizationGroupIds())) {
            todo.getAssigneeGroups().clear();
            for (Long groupId : request.organizationGroupIds()) {
                if (groupId == null) {
                    continue;
                }
                todo.getAssigneeGroups()
                        .add(
                                TodoAssigneeGroup.builder()
                                        .tenantId(request.tenantId())
                                        .todoId(todo.getId())
                                        .organizationGroupId(groupId)
                                        .todo(todo)
                                        .build());
            }
        }
        replaceExcludedUsers(todo, request.excludedUserIds());
        if (request.assigneeUserId() != null) {
            todo.setAssigneeUserId(request.assigneeUserId());
        }
    }

    private void reopen(Todo todo, boolean suppressCreateNotification) {
        todo.setStatus(TodoStatus.OPEN);
        todo.setCompletedAt(null);
        todo.setCompletedByUserId(null);
        todo.setArchivedAt(null);
        Todo saved = todoRepository.save(todo);
        eventPublisher.publishEvent(new TodoUpdatedEvent(saved.getId()));
        eventPublisher.publishEvent(
                new TodoCreatedEvent(saved.getId(), suppressCreateNotification));
    }

    @Transactional
    public void completeOpenBySourceAndType(
            TodoSourceEntityType sourceType, Long sourceId, TodoType type, Long completedByUserId) {
        finalizeOpenBySourceAndType(
                sourceType, sourceId, type, TodoStatus.COMPLETED, completedByUserId);
    }

    @Transactional
    public void cancelOpenBySourceAndType(
            TodoSourceEntityType sourceType, Long sourceId, TodoType type, Long completedByUserId) {
        finalizeOpenBySourceAndType(
                sourceType, sourceId, type, TodoStatus.CANCELLED, completedByUserId);
    }

    /**
     * Cancels open todos for an assignee of the given type/source whose {@code sourceEntityId} is
     * not in {@code keepSourceIds} (e.g. certification reminders after an assignment was removed).
     */
    @Transactional
    public void cancelOpenByAssigneeTypeAndSourceExcept(
            Long assigneeUserId,
            TodoType type,
            TodoSourceEntityType sourceType,
            Set<Long> keepSourceIds,
            Long completedByUserId) {
        if (assigneeUserId == null || type == null || sourceType == null) {
            return;
        }
        Set<Long> keep = keepSourceIds != null ? keepSourceIds : Set.of();
        for (Todo todo :
                todoRepository.findOpenByAssigneeTypeAndSourceType(
                        assigneeUserId, type, sourceType)) {
            if (todo.getSourceEntityId() != null && keep.contains(todo.getSourceEntityId())) {
                continue;
            }
            archive(todo, TodoStatus.CANCELLED, completedByUserId);
        }
    }

    @Transactional
    public void reopenInputChangeTodo(TodoOpenRequest request) {
        validateOpenRequest(request);
        if (request.assigneeUserId() == null) {
            throw new IllegalArgumentException("assigneeUserId is required for input-change todos");
        }
        List<Todo> open =
                todoRepository.findOpenBySourceTypeAndAssignee(
                        request.sourceEntityType(),
                        request.sourceEntityId(),
                        TodoType.WORKFLOW_INPUT_CHANGE_REQUIRED,
                        request.assigneeUserId());
        if (!open.isEmpty()) {
            syncOpenTodoFromRequest(open.getFirst(), request);
            return;
        }
        open(request);
    }

    private void finalizeOpenBySourceAndType(
            TodoSourceEntityType sourceType,
            Long sourceId,
            TodoType type,
            TodoStatus status,
            Long completedByUserId) {
        if (sourceType == null || sourceId == null || type == null) {
            return;
        }
        for (Todo todo : todoRepository.findOpenBySource(sourceType, sourceId)) {
            if (todo.getType() == type) {
                archive(todo, status, completedByUserId);
            }
        }
    }

    private Optional<Todo> findOpenBySourceParticipant(
            TodoSourceEntityType sourceType, Long sourceId, TodoType type, Long participantId) {
        if (sourceType == null || sourceId == null || type == null || participantId == null) {
            return Optional.empty();
        }
        return todoRepository.findOpenBySourceTypeAndParticipant(
                sourceType.name(), sourceId, type.name(), participantId);
    }
}
