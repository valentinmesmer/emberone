/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.list;

public enum TodoListStatus {
    /** Open todos, newest first. */
    OPEN("createdAt,desc"),
    /** Completed / archived todos, most recently archived first. */
    DONE("archivedAt,desc");

    private final String sort;

    TodoListStatus(String sort) {
        this.sort = sort;
    }

    /** Sort expression passed as {@code sortOverride} to the list definition. */
    public String sort() {
        return sort;
    }
}
