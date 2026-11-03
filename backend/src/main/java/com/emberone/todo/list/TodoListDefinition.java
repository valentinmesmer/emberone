/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.todo.list;

import com.emberone.platform.web.list.ListDefinition;
import com.emberone.todo.domain.Todo;

public final class TodoListDefinition {

    public static final ListDefinition<Todo> INSTANCE =
            ListDefinition.<Todo>forEntity(Todo.class)
                    .sortKey("createdAt", "createdAt")
                    .sortKey("archivedAt", "archivedAt")
                    .defaultSort("createdAt,desc")
                    .build();

    private TodoListDefinition() {}
}
