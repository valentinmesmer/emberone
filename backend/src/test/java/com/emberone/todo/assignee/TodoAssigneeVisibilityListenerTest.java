/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.assignee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.emberone.todo.domain.Todo;
import com.emberone.todo.domain.TodoType;
import com.emberone.todo.event.TodoAssigneeGroupsChangedEvent;
import com.emberone.todo.event.TodoVisibilityGainedEvent;
import com.emberone.todo.realtime.TodoRecipientResolver;
import com.emberone.todo.repository.TodoRepository;
import com.emberone.todo.type.TodoTypeDefinition;
import com.emberone.todo.type.TodoTypeDefinitionRegistry;
import com.emberone.userorganizationgroupmembership.event.OrganizationGroupMembershipActivatedEvent;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TodoAssigneeVisibilityListenerTest {

    @Mock TodoRepository todoRepository;
    @Mock TodoTypeDefinitionRegistry typeDefinitions;
    @Mock TodoRecipientResolver recipientResolver;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock TodoTypeDefinition typeDefinition;

    @InjectMocks TodoAssigneeVisibilityListener listener;

    @Test
    void onMembershipActivatedPublishesVisibilityGainedForEligibleMember() {
        Todo todo = Todo.builder().id(10L).type(TodoType.WORKFLOW_START_REQUESTED).build();
        when(todoRepository.findOpenByOrganizationGroupId(30L)).thenReturn(List.of(todo));
        when(typeDefinitions.require(TodoType.WORKFLOW_START_REQUESTED)).thenReturn(typeDefinition);
        when(typeDefinition.notifiesOnCreate()).thenReturn(true);
        when(recipientResolver.resolveRecipientUserIds(todo)).thenReturn(Set.of(42L, 7L));

        listener.onOrganizationGroupMembershipActivated(
                new OrganizationGroupMembershipActivatedEvent(42L, 30L));

        verify(eventPublisher).publishEvent(new TodoVisibilityGainedEvent(10L, Set.of(42L)));
    }

    @Test
    void onMembershipActivatedSkipsWhenUserNotEligibleRecipient() {
        Todo todo = Todo.builder().id(10L).type(TodoType.WORKFLOW_START_REQUESTED).build();
        when(todoRepository.findOpenByOrganizationGroupId(30L)).thenReturn(List.of(todo));
        when(typeDefinitions.require(TodoType.WORKFLOW_START_REQUESTED)).thenReturn(typeDefinition);
        when(typeDefinition.notifiesOnCreate()).thenReturn(true);
        when(recipientResolver.resolveRecipientUserIds(todo)).thenReturn(Set.of(7L));

        listener.onOrganizationGroupMembershipActivated(
                new OrganizationGroupMembershipActivatedEvent(42L, 30L));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void onAssigneeGroupsChangedPublishesOnlyGainedMembers() {
        Todo todo = Todo.builder().id(10L).type(TodoType.WORKFLOW_START_REQUESTED).build();
        when(todoRepository.findById(10L)).thenReturn(Optional.of(todo));
        when(typeDefinitions.require(TodoType.WORKFLOW_START_REQUESTED)).thenReturn(typeDefinition);
        when(typeDefinition.notifiesOnCreate()).thenReturn(true);
        when(recipientResolver.resolveGroupRecipientUserIds(todo, Set.of(1L)))
                .thenReturn(Set.of(7L));
        when(recipientResolver.resolveRecipientUserIds(todo)).thenReturn(Set.of(7L, 42L));

        listener.onTodoAssigneeGroupsChanged(
                new TodoAssigneeGroupsChangedEvent(10L, Set.of(1L), Set.of(2L)));

        ArgumentCaptor<TodoVisibilityGainedEvent> captor =
                ArgumentCaptor.forClass(TodoVisibilityGainedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().todoId()).isEqualTo(10L);
        assertThat(captor.getValue().recipientUserIds()).containsExactly(42L);
    }

    @Test
    void onAssigneeGroupsChangedNoOpsWhenGroupsUnchanged() {
        listener.onTodoAssigneeGroupsChanged(
                new TodoAssigneeGroupsChangedEvent(10L, Set.of(1L), Set.of(1L)));

        verifyNoInteractions(todoRepository, eventPublisher);
    }
}
