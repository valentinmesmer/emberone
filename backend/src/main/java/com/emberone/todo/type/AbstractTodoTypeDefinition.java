/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.todo.type;

import com.emberone.todo.domain.TodoType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractTodoTypeDefinition implements TodoTypeDefinition {

    private final TodoType type;
    private final boolean allowsManualComplete;
    private final boolean notifiesOnCreate;

    @Override
    public TodoType type() {
        return type;
    }

    @Override
    public boolean allowsManualComplete() {
        return allowsManualComplete;
    }

    @Override
    public boolean notifiesOnCreate() {
        return notifiesOnCreate;
    }
}
