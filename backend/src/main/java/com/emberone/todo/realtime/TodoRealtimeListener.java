/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.realtime;

import com.emberone.platform.event.AfterCommitCallback;
import com.emberone.todo.domain.Todo;
import com.emberone.todo.event.TodoCreatedEvent;
import com.emberone.todo.event.TodoUpdatedEvent;
import com.emberone.todo.event.TodoVisibilityGainedEvent;
import com.emberone.todo.exception.TodoNotFoundException;
import com.emberone.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class TodoRealtimeListener {

    private final TodoRepository todoRepository;
    private final TodoRealtimeNotifier realtimeNotifier;

    @AfterCommitCallback
    public void onTodoCreated(TodoCreatedEvent event) {
        Todo todo = requireTodo(event.todoId());
        realtimeNotifier.notifyCreated(todo);
    }

    @AfterCommitCallback
    public void onTodoUpdated(TodoUpdatedEvent event) {
        Todo todo = requireTodo(event.todoId());
        realtimeNotifier.notifyUpdated(todo);
    }

    @AfterCommitCallback
    public void onTodoVisibilityGained(TodoVisibilityGainedEvent event) {
        if (event.todoId() == null || CollectionUtils.isEmpty(event.recipientUserIds())) {
            return;
        }
        Todo todo = requireTodo(event.todoId());
        realtimeNotifier.notifyCreated(todo, event.recipientUserIds());
    }

    private Todo requireTodo(Long todoId) {
        return todoRepository.findById(todoId).orElseThrow(() -> new TodoNotFoundException(todoId));
    }
}
