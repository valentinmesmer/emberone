/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.notification;

import static org.mockito.Mockito.*;

import com.emberone.todo.domain.Todo;
import com.emberone.todo.domain.TodoType;
import com.emberone.todo.event.TodoCreatedEvent;
import com.emberone.todo.event.TodoVisibilityGainedEvent;
import com.emberone.todo.repository.TodoRepository;
import com.emberone.todo.type.TodoTypeDefinition;
import com.emberone.todo.type.TodoTypeDefinitionRegistry;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TodoNotificationListenerTest {

    @Mock TodoRepository todoRepository;
    @Mock TodoNotificationNotifier notificationNotifier;
    @Mock TodoTypeDefinitionRegistry typeDefinitions;
    @Mock TodoTypeDefinition typeDefinition;

    @InjectMocks TodoNotificationListener listener;

    @Test
    void onTodoCreatedNotifiesWhenTypeAllows() {
        Todo todo = Todo.builder().id(1L).type(TodoType.WORKFLOW_START_REQUESTED).build();
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));
        when(typeDefinitions.require(TodoType.WORKFLOW_START_REQUESTED)).thenReturn(typeDefinition);
        when(typeDefinition.notifiesOnCreate()).thenReturn(true);

        listener.onTodoCreated(new TodoCreatedEvent(1L));

        verify(notificationNotifier).notifyCreated(todo);
    }

    @Test
    void onTodoCreatedSkipsWhenSuppressedOnEvent() {
        listener.onTodoCreated(new TodoCreatedEvent(1L, true));

        verifyNoInteractions(todoRepository, notificationNotifier, typeDefinitions);
    }

    @Test
    void onTodoVisibilityGainedNotifiesTargetedRecipients() {
        Todo todo = Todo.builder().id(10L).type(TodoType.WORKFLOW_START_REQUESTED).build();
        when(todoRepository.findById(10L)).thenReturn(Optional.of(todo));
        when(typeDefinitions.require(TodoType.WORKFLOW_START_REQUESTED)).thenReturn(typeDefinition);
        when(typeDefinition.notifiesOnCreate()).thenReturn(true);

        listener.onTodoVisibilityGained(new TodoVisibilityGainedEvent(10L, Set.of(42L)));

        verify(notificationNotifier).notifyCreated(todo, Set.of(42L));
    }

    @Test
    void onTodoVisibilityGainedSkipsWhenTypeDoesNotNotify() {
        Todo todo = Todo.builder().id(10L).type(TodoType.WORKFLOW_APPROVAL_REQUIRED).build();
        when(todoRepository.findById(10L)).thenReturn(Optional.of(todo));
        when(typeDefinitions.require(TodoType.WORKFLOW_APPROVAL_REQUIRED))
                .thenReturn(typeDefinition);
        when(typeDefinition.notifiesOnCreate()).thenReturn(false);

        listener.onTodoVisibilityGained(new TodoVisibilityGainedEvent(10L, Set.of(42L)));

        verify(notificationNotifier, never()).notifyCreated(any(Todo.class), any());
    }
}
