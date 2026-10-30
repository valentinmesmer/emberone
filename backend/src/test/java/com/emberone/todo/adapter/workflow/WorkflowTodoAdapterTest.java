/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.adapter.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.todo.domain.TodoSourceEntityType;
import com.emberone.todo.domain.TodoType;
import com.emberone.todo.service.TodoOpenRequest;
import com.emberone.todo.service.TodoService;
import com.emberone.user.repository.UserRepository;
import com.emberone.workflow.domain.*;
import com.emberone.workflow.event.WorkflowEventContext;
import com.emberone.workflow.event.WorkflowLifecycleEvent;
import com.emberone.workflow.service.WorkflowSelfApproveService;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowTodoAdapterTest {

    @Mock TodoService todoService;
    @Mock UserRepository userRepository;
    @Mock OrganizationGroupRepository organizationGroupRepository;
    @Mock WorkflowSelfApproveService selfApproveService;

    @InjectMocks WorkflowTodoAdapter adapter;

    private static Workflow workflow(Long id) {
        return Workflow.builder()
                .id(id)
                .tenantId(1L)
                .workflowTypeId(1L)
                .openedByUserId(2L)
                .participants(new LinkedHashSet<>())
                .build();
    }

    private static Workflow workflowWithParticipants(WorkflowParticipant... participants) {
        Workflow workflow = workflow(1L);
        workflow.setParticipants(new LinkedHashSet<>(Set.of(participants)));
        return workflow;
    }

    private static WorkflowParticipant orgGroupParticipant(Long groupId, Long claimerUserId) {
        return orgGroupParticipant(groupId, claimerUserId, ParticipantSlotStatus.PENDING);
    }

    private static WorkflowParticipant orgGroupParticipant(
            Long groupId, Long claimerUserId, ParticipantSlotStatus slotStatus) {
        return WorkflowParticipant.builder()
                .id(7L)
                .role(ParticipantRole.APPROVER)
                .principalType(PrincipalType.ORGANIZATION_GROUP)
                .principalId(groupId)
                .slotStatus(slotStatus)
                .claimedByUserId(claimerUserId)
                .build();
    }

    private static WorkflowParticipant userParticipant(Long userId) {
        return WorkflowParticipant.builder()
                .id(8L)
                .role(ParticipantRole.APPROVER)
                .principalType(PrincipalType.USER)
                .principalId(userId)
                .slotStatus(ParticipantSlotStatus.PENDING)
                .build();
    }

    @Test
    void onOpenedSkipsAutoApprovedOrgGroupSlot() {
        Workflow workflow =
                workflowWithParticipants(
                        orgGroupParticipant(5L, null, ParticipantSlotStatus.APPROVED));

        adapter.onEvent(workflow, WorkflowLifecycleEvent.OPENED, WorkflowEventContext.empty());

        verify(todoService, never()).reopenGroupClaimTodo(any());
        verify(todoService, never()).reopenApprovalTodo(any());
    }

    @Test
    void onOpenedCreatesGroupTodoForUnclaimedOrgGroupSlot() {
        Workflow workflow = workflowWithParticipants(orgGroupParticipant(5L, null));
        when(selfApproveService.isAllowed(eq(workflow), any())).thenReturn(true);

        adapter.onEvent(workflow, WorkflowLifecycleEvent.OPENED, WorkflowEventContext.empty());

        ArgumentCaptor<TodoOpenRequest> captor = ArgumentCaptor.forClass(TodoOpenRequest.class);
        verify(todoService).reopenGroupClaimTodo(captor.capture());
        TodoOpenRequest request = captor.getValue();
        assertThat(request.type()).isEqualTo(TodoType.WORKFLOW_START_REQUESTED);
        assertThat(request.organizationGroupIds()).containsExactly(5L);
        assertThat(request.excludedUserIds()).isEmpty();
    }

    @Test
    void onOpenedExcludesOpenerFromGroupTodoWhenSelfApproveBlocked() {
        Workflow workflow = workflowWithParticipants(orgGroupParticipant(5L, null));
        when(selfApproveService.isAllowed(eq(workflow), any())).thenReturn(false);

        adapter.onEvent(workflow, WorkflowLifecycleEvent.OPENED, WorkflowEventContext.empty());

        ArgumentCaptor<TodoOpenRequest> captor = ArgumentCaptor.forClass(TodoOpenRequest.class);
        verify(todoService).reopenGroupClaimTodo(captor.capture());
        assertThat(captor.getValue().excludedUserIds()).containsExactly(2L);
    }

    @Test
    void onOpenedExcludesOpenerOnlyFromSlotsThatBlockSelfApprove() {
        WorkflowParticipant allowed = orgGroupParticipant(5L, null);
        allowed.setOrganizationId(10L);
        WorkflowParticipant blocked = orgGroupParticipant(6L, null);
        blocked.setOrganizationId(20L);
        Workflow workflow = workflowWithParticipants(allowed, blocked);
        when(selfApproveService.isAllowed(workflow, 10L)).thenReturn(true);
        when(selfApproveService.isAllowed(workflow, 20L)).thenReturn(false);

        adapter.onEvent(workflow, WorkflowLifecycleEvent.OPENED, WorkflowEventContext.empty());

        ArgumentCaptor<TodoOpenRequest> captor = ArgumentCaptor.forClass(TodoOpenRequest.class);
        verify(todoService, times(2)).reopenGroupClaimTodo(captor.capture());
        assertThat(captor.getAllValues())
                .filteredOn(request -> request.organizationGroupIds().contains(5L))
                .singleElement()
                .satisfies(request -> assertThat(request.excludedUserIds()).isEmpty());
        assertThat(captor.getAllValues())
                .filteredOn(request -> request.organizationGroupIds().contains(6L))
                .singleElement()
                .satisfies(request -> assertThat(request.excludedUserIds()).containsExactly(2L));
    }

    @Test
    void onOpenedCreatesUserTodoForUserPrincipal() {
        Workflow workflow = workflowWithParticipants(userParticipant(12L));

        adapter.onEvent(workflow, WorkflowLifecycleEvent.OPENED, WorkflowEventContext.empty());

        ArgumentCaptor<TodoOpenRequest> captor = ArgumentCaptor.forClass(TodoOpenRequest.class);
        verify(todoService).reopenApprovalTodo(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(TodoType.WORKFLOW_APPROVAL_REQUIRED);
        assertThat(captor.getValue().assigneeUserId()).isEqualTo(12L);
        assertThat(captor.getValue().suppressCreateNotification()).isFalse();
    }

    @Test
    void onClaimedArchivesGroupTodoAndOpensApprovalTodo() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = orgGroupParticipant(5L, 99L);
        participant.setId(7L);
        workflow.setParticipants(new LinkedHashSet<>(Set.of(participant)));

        adapter.onEvent(
                workflow, WorkflowLifecycleEvent.CLAIMED, WorkflowEventContext.ofActor(7L, 99L));

        verify(todoService)
                .archiveOpenBySourceParticipant(
                        TodoSourceEntityType.WORKFLOW,
                        1L,
                        TodoType.WORKFLOW_START_REQUESTED,
                        7L,
                        99L);
        ArgumentCaptor<TodoOpenRequest> captor = ArgumentCaptor.forClass(TodoOpenRequest.class);
        verify(todoService).reopenApprovalTodo(captor.capture());
        assertThat(captor.getValue().suppressCreateNotification()).isTrue();
    }

    @Test
    void onUnclaimedCancelsApprovalTodoAndReopensGroupTodo() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = orgGroupParticipant(5L, null);
        participant.setId(7L);
        workflow.setParticipants(new LinkedHashSet<>(Set.of(participant)));

        adapter.onEvent(
                workflow,
                WorkflowLifecycleEvent.UNCLAIMED,
                WorkflowEventContext.unclaimed(7L, 99L));

        verify(todoService)
                .cancelOpenBySourceParticipant(
                        TodoSourceEntityType.WORKFLOW,
                        1L,
                        TodoType.WORKFLOW_APPROVAL_REQUIRED,
                        7L,
                        99L);
        verify(todoService).reopenGroupClaimTodo(any());
    }

    @Test
    void onDecisionArchivesApprovalTodo() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = orgGroupParticipant(5L, 99L);
        participant.setId(7L);
        workflow.setParticipants(new LinkedHashSet<>(Set.of(participant)));

        adapter.onEvent(
                workflow,
                WorkflowLifecycleEvent.DECISION_RECORDED,
                WorkflowEventContext.ofActor(7L, 99L));

        verify(todoService)
                .archiveOpenBySourceParticipant(
                        TodoSourceEntityType.WORKFLOW,
                        1L,
                        TodoType.WORKFLOW_APPROVAL_REQUIRED,
                        7L,
                        99L);
    }

    @Test
    void onApproverSlotRemovedCancelsOpenTodosForParticipant() {
        Workflow workflow = workflow(1L);

        adapter.onEvent(
                workflow,
                WorkflowLifecycleEvent.APPROVER_SLOT_REMOVED,
                WorkflowEventContext.ofActor(7L, null));

        verify(todoService)
                .cancelOpenTodosForWorkflowParticipant(TodoSourceEntityType.WORKFLOW, 1L, 7L, null);
    }

    @Test
    void onRejectedCancelsBySource() {
        Workflow workflow = workflow(1L);
        adapter.onEvent(workflow, WorkflowLifecycleEvent.REJECTED, WorkflowEventContext.empty());
        verify(todoService).cancelBySource(TodoSourceEntityType.WORKFLOW, 1L, null);
    }

    @Test
    void onInputChangeRequestedOpensOpenerTodoAndCancelsApproverTodos() {
        Workflow workflow = workflow(1L);
        workflow.getInputChanges()
                .add(
                        WorkflowInputChange.builder()
                                .tenantId(1L)
                                .workflow(workflow)
                                .requestedByUserId(99L)
                                .message("Bitte Datum korrigieren")
                                .status(WorkflowInputChangeStatus.OPEN)
                                .build());

        adapter.onEvent(
                workflow,
                WorkflowLifecycleEvent.INPUT_CHANGE_REQUESTED,
                WorkflowEventContext.empty());

        verify(todoService)
                .cancelOpenBySourceAndType(
                        TodoSourceEntityType.WORKFLOW,
                        1L,
                        TodoType.WORKFLOW_APPROVAL_REQUIRED,
                        null);
        verify(todoService)
                .cancelOpenBySourceAndType(
                        TodoSourceEntityType.WORKFLOW, 1L, TodoType.WORKFLOW_START_REQUESTED, null);
        ArgumentCaptor<TodoOpenRequest> captor = ArgumentCaptor.forClass(TodoOpenRequest.class);
        verify(todoService).reopenInputChangeTodo(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(TodoType.WORKFLOW_INPUT_CHANGE_REQUIRED);
        assertThat(captor.getValue().assigneeUserId()).isEqualTo(2L);
        assertThat(captor.getValue().params())
                .containsEntry("inputChangeMessage", "Bitte Datum korrigieren");
    }

    @Test
    void onInputUpdatedCompletesInputChangeTodoAndRestoresApproverTodos() {
        WorkflowParticipant pendingClaimed = orgGroupParticipant(30L, 99L);
        Workflow workflow = workflowWithParticipants(pendingClaimed);
        workflow.setStatus(WorkflowStatus.OPEN);

        adapter.onEvent(
                workflow, WorkflowLifecycleEvent.INPUT_UPDATED, WorkflowEventContext.empty());

        verify(todoService)
                .completeOpenBySourceAndType(
                        TodoSourceEntityType.WORKFLOW,
                        1L,
                        TodoType.WORKFLOW_INPUT_CHANGE_REQUIRED,
                        2L);
        verify(todoService).reopenApprovalTodo(any());
    }

    @Test
    void onInputChangeWithdrawnCancelsInputChangeTodoAndRestoresApproverTodos() {
        WorkflowParticipant pendingClaimed = orgGroupParticipant(30L, 99L);
        Workflow workflow = workflowWithParticipants(pendingClaimed);
        workflow.setStatus(WorkflowStatus.OPEN);

        adapter.onEvent(
                workflow,
                WorkflowLifecycleEvent.INPUT_CHANGE_WITHDRAWN,
                WorkflowEventContext.empty());

        verify(todoService)
                .cancelOpenBySourceAndType(
                        TodoSourceEntityType.WORKFLOW,
                        1L,
                        TodoType.WORKFLOW_INPUT_CHANGE_REQUIRED,
                        2L);
        verify(todoService).reopenApprovalTodo(any());
    }
}
