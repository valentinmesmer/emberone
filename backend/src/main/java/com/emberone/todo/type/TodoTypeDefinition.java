/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.type;

import com.emberone.todo.domain.TodoType;

public interface TodoTypeDefinition {

    TodoType type();

    boolean allowsManualComplete();

    /** Whether creating this todo should trigger a {@code TODO_ASSIGNED} notification. */
    boolean notifiesOnCreate();
}
