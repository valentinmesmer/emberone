/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.todo.event;

/**
 * Published after a to-do has been opened or restored so realtime and notification adapters can
 * subscribe without {@code TodoService} depending on them directly.
 *
 * @param suppressCreateNotification when true, skip {@code TODO_ASSIGNED} (e.g. after claim);
 *     realtime {@code todo.created} is still emitted
 */
public record TodoCreatedEvent(Long todoId, boolean suppressCreateNotification) {

    public TodoCreatedEvent(Long todoId) {
        this(todoId, false);
    }
}
