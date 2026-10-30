/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.list;

import com.emberone.todo.domain.Todo;
import com.emberone.todo.domain.TodoAssigneeGroup;
import com.emberone.todo.domain.TodoExcludedUser;
import com.emberone.todo.domain.TodoStatus;
import com.emberone.todo.domain.TodoType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class TodoListSpecs {

    private TodoListSpecs() {}

    public static Specification<Todo> visibleToUser(Long userId, List<Long> groupIds) {
        return (root, query, cb) -> {
            var assigneeUser = cb.equal(root.get("assigneeUserId"), userId);
            if (groupIds == null || groupIds.isEmpty()) {
                return assigneeUser;
            }
            Subquery<Long> groupMatch = query.subquery(Long.class);
            Root<TodoAssigneeGroup> groupRoot = groupMatch.from(TodoAssigneeGroup.class);
            groupMatch.select(cb.literal(1L));
            groupMatch.where(
                    cb.equal(groupRoot.get("todo"), root),
                    groupRoot.get("organizationGroupId").in(groupIds));
            return cb.or(assigneeUser, cb.exists(groupMatch));
        };
    }

    public static Specification<Todo> notExcludedForUser(Long userId) {
        return (root, query, cb) -> {
            Subquery<Long> excluded = query.subquery(Long.class);
            Root<TodoExcludedUser> excludedRoot = excluded.from(TodoExcludedUser.class);
            excluded.select(cb.literal(1L));
            excluded.where(
                    cb.equal(excludedRoot.get("todo"), root),
                    cb.equal(excludedRoot.get("userId"), userId));
            return cb.not(cb.exists(excluded));
        };
    }

    public static Specification<Todo> statusFilter(TodoListStatus status, Long userId) {
        Specification<Todo> statusSpec =
                switch (status) {
                    case OPEN ->
                            Specification.allOf(
                                    (root, query, cb) -> cb.isNull(root.get("archivedAt")),
                                    (root, query, cb) ->
                                            cb.equal(root.get("status"), TodoStatus.OPEN),
                                    notExcludedForUser(userId));
                    case DONE -> (root, query, cb) -> cb.isNotNull(root.get("archivedAt"));
                };
        return statusSpec;
    }

    public static Specification<Todo> statusesFilter(List<TodoListStatus> statuses, Long userId) {
        if (statuses == null || statuses.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        List<Specification<Todo>> buckets = new ArrayList<>();
        for (TodoListStatus status : statuses) {
            buckets.add(statusFilter(status, userId));
        }
        return Specification.anyOf(buckets);
    }

    public static Specification<Todo> typesFilter(List<TodoType> types) {
        if (types == null || types.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> root.get("type").in(types);
    }
}
