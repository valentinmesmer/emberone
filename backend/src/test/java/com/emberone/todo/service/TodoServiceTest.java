/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.emberone.platform.navigation.domain.AppLink;
import com.emberone.todo.domain.*;
import com.emberone.todo.event.TodoAssigneeGroupsChangedEvent;
import com.emberone.todo.event.TodoCreatedEvent;
import com.emberone.todo.event.TodoUpdatedEvent;
import com.emberone.todo.repository.TodoRepository;
import com.emberone.todo.type.TodoTypeDefinition;
import com.emberone.todo.type.TodoTypeDefinitionRegistry;
import java.util.List;
import java.util.Map;
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
class TodoServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long WORKFLOW_ID = 42L;
    private static final Long PARTICIPANT_ID = 7L;

    @Mock TodoRepository todoRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock TodoTypeDefinitionRegistry typeDefinitions;

    @InjectMocks TodoService todoService;

    @Test
    void openCreatesUserApprovalTodoAndPublishesCreatedEvent() {
        when(todoRepository.findOpenBySourceTypeAndParticipant(
                        anyString(), anyLong(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(todoRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            Todo todo = invocation.getArgument(0);
                            todo.setId(10L);
                            return todo;
                        });

        Todo result =
                todoService.open(
                        TodoOpenRequest.builder()
                                .tenantId(TENANT_ID)
                                .type(TodoType.WORKFLOW_APPROVAL_REQUIRED)
                                .assigneeUserId(99L)
                                .params(Map.of(TodoParamKeys.PARTICIPANT_ID, PARTICIPANT_ID))
                                .link(AppLink.workflow(WORKFLOW_ID))
                                .sourceEntityType(TodoSourceEntityType.WORKFLOW)
                                .sourceEntityId(WORKFLOW_ID)
                                .build());

        assertThat(result.getAssigneeUserId()).isEqualTo(99L);
        assertThat(result.getStatus()).isEqualTo(TodoStatus.OPEN);
        verify(eventPublisher).publishEvent(new TodoCreatedEvent(10L));
    }

    @Test
    void openCreatesGroupClaimTodoAndSendsNotification() {
        when(todoRepository.findOpenBySourceTypeAndParticipant(
                        anyString(), anyLong(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(todoRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            Todo todo = invocation.getArgument(0);
                            if (todo.getId() == null) {
                                todo.setId(11L);
                            }
                            return todo;
                        });

        Todo result =
                todoService.open(
                        TodoOpenRequest.builder()
                                .tenantId(TENANT_ID)
                                .type(TodoType.WORKFLOW_START_REQUESTED)
                                .organizationGroupIds(Set.of(5L))
                                .params(Map.of(TodoParamKeys.PARTICIPANT_ID, PARTICIPANT_ID))
                                .link(AppLink.workflow(WORKFLOW_ID))
                                .sourceEntityType(TodoSourceEntityType.WORKFLOW)
                                .sourceEntityId(WORKFLOW_ID)
                                .build());

        verify(eventPublisher).publishEvent(new TodoCreatedEvent(11L));
    }

    @Test
    void openCreatesGroupTodoWithAssigneeGroups() {
        when(todoRepository.findOpenBySourceTypeAndParticipant(
                        anyString(), anyLong(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(todoRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            Todo todo = invocation.getArgument(0);
                            if (todo.getId() == null) {
                                todo.setId(11L);
                            }
                            return todo;
                        });
        Todo result =
                todoService.open(
                        TodoOpenRequest.builder()
                                .tenantId(TENANT_ID)
                                .type(TodoType.WORKFLOW_START_REQUESTED)
                                .organizationGroupIds(Set.of(5L, 6L))
                                .params(Map.of(TodoParamKeys.PARTICIPANT_ID, PARTICIPANT_ID))
                                .link(AppLink.workflow(WORKFLOW_ID))
                                .sourceEntityType(TodoSourceEntityType.WORKFLOW)
                                .sourceEntityId(WORKFLOW_ID)
                                .build());

        assertThat(result.getAssigneeGroups()).hasSize(2);
        assertThat(result.getAssigneeGroups()).allMatch(group -> group.getTodoId().equals(11L));
        assertThat(result.getAssigneeUserId()).isNull();
    }

    @Test
    void openRequiresAtLeastOneAssignee() {
        assertThatThrownBy(
                        () ->
                                todoService.open(
                                        TodoOpenRequest.builder()
                                                .tenantId(TENANT_ID)
                                                .type(TodoType.WORKFLOW_START_REQUESTED)
                                                .sourceEntityType(TodoSourceEntityType.WORKFLOW)
                                                .sourceEntityId(WORKFLOW_ID)
                                                .build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void completeBySourceArchivesAllOpenTodos() {
        Todo open =
                Todo.builder()
                        .id(1L)
                        .tenantId(TENANT_ID)
                        .status(TodoStatus.OPEN)
                        .type(TodoType.WORKFLOW_APPROVAL_REQUIRED)
                        .assigneeUserId(2L)
                        .build();
        when(todoRepository.findOpenBySource(TodoSourceEntityType.WORKFLOW, WORKFLOW_ID))
                .thenReturn(List.of(open));
        when(todoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        todoService.completeBySource(TodoSourceEntityType.WORKFLOW, WORKFLOW_ID, 2L);

        ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TodoStatus.COMPLETED);
        assertThat(captor.getValue().getArchivedAt()).isNotNull();
        verify(eventPublisher).publishEvent(new TodoUpdatedEvent(1L));
    }

    @Test
    void cancelBySourceMarksTodosCancelled() {
        Todo open =
                Todo.builder()
                        .id(1L)
                        .tenantId(TENANT_ID)
                        .status(TodoStatus.OPEN)
                        .type(TodoType.WORKFLOW_START_REQUESTED)
                        .build();
        when(todoRepository.findOpenBySource(TodoSourceEntityType.WORKFLOW, WORKFLOW_ID))
                .thenReturn(List.of(open));
        when(todoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        todoService.cancelBySource(TodoSourceEntityType.WORKFLOW, WORKFLOW_ID, null);

        ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TodoStatus.CANCELLED);
    }

    @Test
    void reopenGroupClaimTodoUpdatesExcludedUsersOnExistingOpenTodo() {
        Todo open =
                Todo.builder()
                        .id(5L)
                        .tenantId(TENANT_ID)
                        .status(TodoStatus.OPEN)
                        .type(TodoType.WORKFLOW_START_REQUESTED)
                        .params(
                                new java.util.HashMap<>(
                                        Map.of(TodoParamKeys.PARTICIPANT_ID, PARTICIPANT_ID)))
                        .sourceEntityType(TodoSourceEntityType.WORKFLOW)
                        .sourceEntityId(WORKFLOW_ID)
                        .assigneeGroups(
                                new java.util.ArrayList<>(
                                        List.of(
                                                TodoAssigneeGroup.builder()
                                                        .organizationGroupId(5L)
                                                        .build())))
                        .excludedUsers(new java.util.ArrayList<>())
                        .build();
        when(todoRepository.findOpenBySourceTypeAndParticipant(
                        TodoSourceEntityType.WORKFLOW.name(),
                        WORKFLOW_ID,
                        TodoType.WORKFLOW_START_REQUESTED.name(),
                        PARTICIPANT_ID))
                .thenReturn(Optional.of(open));
        when(todoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        todoService.reopenGroupClaimTodo(
                TodoOpenRequest.builder()
                        .tenantId(TENANT_ID)
                        .type(TodoType.WORKFLOW_START_REQUESTED)
                        .organizationGroupIds(Set.of(5L))
                        .excludedUserIds(Set.of(42L))
                        .params(Map.of(TodoParamKeys.PARTICIPANT_ID, PARTICIPANT_ID))
                        .link(AppLink.workflow(WORKFLOW_ID))
                        .sourceEntityType(TodoSourceEntityType.WORKFLOW)
                        .sourceEntityId(WORKFLOW_ID)
                        .build());

        assertThat(open.getExcludedUsers())
                .extracting(TodoExcludedUser::getUserId)
                .containsExactly(42L);
        verify(eventPublisher).publishEvent(new TodoUpdatedEvent(5L));
        verify(eventPublisher, never())
                .publishEvent(
                        org.mockito.ArgumentMatchers.isA(TodoAssigneeGroupsChangedEvent.class));
    }

    @Test
    void reopenGroupClaimTodoPublishesAssigneeGroupsChangedWhenGroupRemapped() {
        Todo open =
                Todo.builder()
                        .id(5L)
                        .tenantId(TENANT_ID)
                        .status(TodoStatus.OPEN)
                        .type(TodoType.WORKFLOW_START_REQUESTED)
                        .params(
                                new java.util.HashMap<>(
                                        Map.of(TodoParamKeys.PARTICIPANT_ID, PARTICIPANT_ID)))
                        .sourceEntityType(TodoSourceEntityType.WORKFLOW)
                        .sourceEntityId(WORKFLOW_ID)
                        .assigneeGroups(
                                new java.util.ArrayList<>(
                                        List.of(
                                                TodoAssigneeGroup.builder()
                                                        .organizationGroupId(5L)
                                                        .build())))
                        .build();
        when(todoRepository.findOpenBySourceTypeAndParticipant(
                        TodoSourceEntityType.WORKFLOW.name(),
                        WORKFLOW_ID,
                        TodoType.WORKFLOW_START_REQUESTED.name(),
                        PARTICIPANT_ID))
                .thenReturn(Optional.of(open));
        when(todoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        todoService.reopenGroupClaimTodo(
                TodoOpenRequest.builder()
                        .tenantId(TENANT_ID)
                        .type(TodoType.WORKFLOW_START_REQUESTED)
                        .organizationGroupIds(Set.of(9L))
                        .params(Map.of(TodoParamKeys.PARTICIPANT_ID, PARTICIPANT_ID))
                        .link(AppLink.workflow(WORKFLOW_ID))
                        .sourceEntityType(TodoSourceEntityType.WORKFLOW)
                        .sourceEntityId(WORKFLOW_ID)
                        .build());

        verify(eventPublisher).publishEvent(new TodoUpdatedEvent(5L));
        verify(eventPublisher)
                .publishEvent(new TodoAssigneeGroupsChangedEvent(5L, Set.of(5L), Set.of(9L)));
    }

    @Test
    void manualCompleteAllowedForWorkflowApprovalType() {
        TodoTypeDefinition definition = org.mockito.Mockito.mock(TodoTypeDefinition.class);
        when(definition.allowsManualComplete()).thenReturn(true);
        when(typeDefinitions.require(TodoType.WORKFLOW_APPROVAL_REQUIRED)).thenReturn(definition);

        Todo open =
                Todo.builder()
                        .id(3L)
                        .tenantId(TENANT_ID)
                        .status(TodoStatus.OPEN)
                        .type(TodoType.WORKFLOW_APPROVAL_REQUIRED)
                        .build();
        when(todoRepository.findById(3L)).thenReturn(Optional.of(open));
        when(todoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Todo completed = todoService.completeManually(3L, 9L);

        assertThat(completed.getStatus()).isEqualTo(TodoStatus.COMPLETED);
        assertThat(completed.getCompletedByUserId()).isEqualTo(9L);
    }
}
