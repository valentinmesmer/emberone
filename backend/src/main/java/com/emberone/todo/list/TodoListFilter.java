/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.list;

import com.emberone.platform.web.list.ListFilters;
import com.emberone.todo.domain.TodoType;
import java.util.List;
import java.util.Optional;

/**
 * Self todo list filters. Repeated {@code status} and {@code type} params are OR-combined within
 * each dimension; empty {@code status} means no status restriction (open and done).
 */
public record TodoListFilter(List<TodoListStatus> statuses, List<TodoType> types) {

    public TodoListFilter {
        if (statuses == null) {
            statuses = List.of();
        }
        if (types == null) {
            types = List.of();
        }
    }

    public static TodoListFilter parse(List<String> status, List<String> type) {
        return new TodoListFilter(
                ListFilters.parseEnumList(
                        status,
                        "status",
                        raw -> {
                            try {
                                return Optional.of(TodoListStatus.valueOf(raw));
                            } catch (IllegalArgumentException ex) {
                                return Optional.empty();
                            }
                        }),
                ListFilters.parseEnumList(
                        type,
                        "type",
                        raw -> {
                            try {
                                return Optional.of(TodoType.valueOf(raw));
                            } catch (IllegalArgumentException ex) {
                                return Optional.empty();
                            }
                        }));
    }

    /** Sort override when exactly one status bucket is selected; otherwise default list order. */
    public String resolveSort() {
        if (statuses.size() == 1) {
            return statuses.get(0).sort();
        }
        return TodoListStatus.OPEN.sort();
    }
}
