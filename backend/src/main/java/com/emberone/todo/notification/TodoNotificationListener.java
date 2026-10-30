/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.notification;

import com.emberone.platform.event.AfterCommitEventListener;
import com.emberone.todo.domain.Todo;
import com.emberone.todo.event.TodoCreatedEvent;
import com.emberone.todo.event.TodoVisibilityGainedEvent;
import com.emberone.todo.exception.TodoNotFoundException;
import com.emberone.todo.repository.TodoRepository;
import com.emberone.todo.type.TodoTypeDefinitionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class TodoNotificationListener {

    private final TodoRepository todoRepository;
    private final TodoNotificationNotifier notificationNotifier;
    private final TodoTypeDefinitionRegistry typeDefinitions;

    @AfterCommitEventListener
    public void onTodoCreated(TodoCreatedEvent event) {
        if (event.suppressCreateNotification()) {
            return;
        }
        Todo todo = requireTodo(event.todoId());
        if (!typeDefinitions.require(todo.getType()).notifiesOnCreate()) {
            return;
        }
        notificationNotifier.notifyCreated(todo);
    }

    @AfterCommitEventListener
    public void onTodoVisibilityGained(TodoVisibilityGainedEvent event) {
        if (event.todoId() == null || CollectionUtils.isEmpty(event.recipientUserIds())) {
            return;
        }
        Todo todo = requireTodo(event.todoId());
        if (!typeDefinitions.require(todo.getType()).notifiesOnCreate()) {
            return;
        }
        notificationNotifier.notifyCreated(todo, event.recipientUserIds());
    }

    private Todo requireTodo(Long todoId) {
        return todoRepository.findById(todoId).orElseThrow(() -> new TodoNotFoundException(todoId));
    }
}
