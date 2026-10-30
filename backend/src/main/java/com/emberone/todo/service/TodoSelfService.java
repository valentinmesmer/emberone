/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.service;

import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.PageReorder;
import com.emberone.todo.domain.Todo;
import com.emberone.todo.domain.TodoType;
import com.emberone.todo.dto.TodoDto;
import com.emberone.todo.dto.TodoPageDto;
import com.emberone.todo.exception.TodoAccessDeniedException;
import com.emberone.todo.exception.TodoNotFoundException;
import com.emberone.todo.exception.TodoStateException;
import com.emberone.todo.list.TodoListDefinition;
import com.emberone.todo.list.TodoListFilter;
import com.emberone.todo.list.TodoListSpecs;
import com.emberone.todo.mapper.TodoDtoAssembler;
import com.emberone.todo.repository.TodoRepository;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TodoSelfService {

    private final TodoRepository todoRepository;
    private final TodoDtoAssembler todoDtoAssembler;
    private final TodoService todoService;
    private final TokenService tokenService;
    private final UserOrganizationGroupMembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public TodoPageDto listTodos(ListQuery query, TodoListFilter filter) {
        Long userId = tokenService.requireUserId();
        List<Long> groupIds = activeGroupIds(userId);
        long openCount = todoRepository.countOpenVisibleForUser(userId, groupIds);

        Pageable pageable = TodoListDefinition.INSTANCE.pageable(query, filter.resolveSort());

        Specification<Todo> spec =
                Specification.allOf(
                        TodoListSpecs.visibleToUser(userId, groupIds),
                        TodoListSpecs.statusesFilter(filter.statuses(), userId),
                        TodoListSpecs.typesFilter(filter.types()));

        Page<Todo> entityPage = todoRepository.findAll(spec, pageable);
        List<Long> ids = entityPage.getContent().stream().map(Todo::getId).toList();
        List<Todo> hydrated =
                ids.isEmpty() ? List.of() : todoRepository.findWithAssigneeGroupsByIdIn(ids);
        List<Todo> ordered = PageReorder.reorderByIds(ids, hydrated, Todo::getId);
        Page<Todo> result = PageReorder.pageOf(ordered, entityPage);
        return todoDtoAssembler.toPageDto(result, openCount);
    }

    @Transactional(readOnly = true)
    public TodoDto getTodo(Long todoId) {
        Long userId = tokenService.requireUserId();
        List<Long> groupIds = activeGroupIds(userId);
        Todo todo =
                todoRepository
                        .findVisibleByIdForUser(todoId, userId, groupIds)
                        .orElseThrow(() -> new TodoNotFoundException(todoId));
        return todoDtoAssembler.toDto(todo);
    }

    @Transactional(readOnly = true)
    public List<TodoType> listDistinctTypesForUser() {
        Long userId = tokenService.requireUserId();
        List<Long> groupIds = activeGroupIds(userId);
        return todoRepository.findDistinctTypesVisibleToUser(userId, groupIds).stream()
                .sorted(Comparator.comparing(Enum::name))
                .toList();
    }

    @Transactional(readOnly = true)
    public long countOpenTodos() {
        Long userId = tokenService.requireUserId();
        List<Long> groupIds = activeGroupIds(userId);
        return todoRepository.countOpenVisibleForUser(userId, groupIds);
    }

    @Transactional
    public TodoDto complete(Long todoId) {
        Long userId = tokenService.requireUserId();
        List<Long> groupIds = activeGroupIds(userId);
        Todo todo =
                todoRepository
                        .findVisibleByIdForUser(todoId, userId, groupIds)
                        .orElseThrow(() -> new TodoNotFoundException(todoId));
        if (!todo.isOpen()) {
            throw new TodoStateException("Todo is not open");
        }
        if (!isAuthorizedToComplete(todo, userId, groupIds)) {
            throw new TodoAccessDeniedException("Not allowed to complete this todo");
        }
        Todo completed = todoService.completeManually(todoId, userId);
        return todoDtoAssembler.toDto(completed);
    }

    private boolean isAuthorizedToComplete(Todo todo, Long userId, List<Long> groupIds) {
        if (todo.getAssigneeUserId() != null && todo.getAssigneeUserId().equals(userId)) {
            return true;
        }
        return todo.getAssigneeGroups().stream()
                .anyMatch(group -> groupIds.contains(group.getOrganizationGroupId()));
    }

    private List<Long> activeGroupIds(Long userId) {
        Instant now = Instant.now();
        List<Long> groupIds = new ArrayList<>();
        for (UserOrganizationGroupMembership membership :
                membershipRepository.findByUser_Id(userId)) {
            if (membership.isActiveAt(now)) {
                groupIds.add(membership.getOrganizationGroup().getId());
            }
        }
        return groupIds;
    }
}
