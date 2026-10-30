/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.realtime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emberone.todo.domain.Todo;
import com.emberone.todo.event.TodoCreatedEvent;
import com.emberone.todo.event.TodoUpdatedEvent;
import com.emberone.todo.event.TodoVisibilityGainedEvent;
import com.emberone.todo.exception.TodoNotFoundException;
import com.emberone.todo.repository.TodoRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TodoRealtimeListenerTest {

    @Mock TodoRepository todoRepository;
    @Mock TodoRealtimeNotifier realtimeNotifier;

    @InjectMocks TodoRealtimeListener listener;

    @Test
    void onTodoCreatedLoadsTodoWithRecipientCollectionsBeforeNotifying() {
        Todo todo = Todo.builder().id(5L).build();
        when(todoRepository.findByIdWithRecipients(5L)).thenReturn(Optional.of(todo));

        listener.onTodoCreated(new TodoCreatedEvent(5L, false));

        verify(realtimeNotifier).notifyCreated(todo);
    }

    @Test
    void onTodoUpdatedLoadsTodoWithRecipientCollectionsBeforeNotifying() {
        Todo todo = Todo.builder().id(5L).build();
        when(todoRepository.findByIdWithRecipients(5L)).thenReturn(Optional.of(todo));

        listener.onTodoUpdated(new TodoUpdatedEvent(5L));

        verify(realtimeNotifier).notifyUpdated(todo);
    }

    @Test
    void onTodoVisibilityGainedNotifiesOnlyGivenRecipients() {
        Todo todo = Todo.builder().id(5L).build();
        when(todoRepository.findByIdWithRecipients(5L)).thenReturn(Optional.of(todo));

        listener.onTodoVisibilityGained(new TodoVisibilityGainedEvent(5L, Set.of(42L)));

        verify(realtimeNotifier).notifyCreated(todo, Set.of(42L));
    }

    @Test
    void onTodoVisibilityGainedSkipsWithoutRecipients() {
        listener.onTodoVisibilityGained(new TodoVisibilityGainedEvent(5L, Set.of()));

        verifyNoInteractions(todoRepository, realtimeNotifier);
    }

    @Test
    void onTodoUpdatedFailsWhenTodoMissing() {
        when(todoRepository.findByIdWithRecipients(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listener.onTodoUpdated(new TodoUpdatedEvent(9L)))
                .isInstanceOf(TodoNotFoundException.class);
    }
}
