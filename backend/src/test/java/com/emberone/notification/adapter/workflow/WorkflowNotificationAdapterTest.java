/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.adapter.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.service.NotificationRequest;
import com.emberone.notification.service.NotificationService;
import com.emberone.platform.navigation.domain.AppLink;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.workflow.domain.*;
import com.emberone.workflow.event.WorkflowEventContext;
import com.emberone.workflow.event.WorkflowLifecycleEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowNotificationAdapterTest {

    private static final Long TENANT_ID = 7L;
    private static final Long WORKFLOW_ID = 42L;
    private static final Long CREATOR_ID = 10L;
    private static final Long APPROVER_ID = 20L;

    @Mock NotificationService notificationService;
    @Mock UserRepository userRepository;

    @InjectMocks WorkflowNotificationAdapter adapter;

    private static Workflow workflowWithParticipants(WorkflowParticipant... participants) {
        Workflow workflow =
                Workflow.builder()
                        .id(WORKFLOW_ID)
                        .tenantId(TENANT_ID)
                        .workflowTypeId(WorkflowType.NAME_CHANGE.getId())
                        .status(WorkflowStatus.OPEN)
                        .openedByUserId(CREATOR_ID)
                        .build();
        for (WorkflowParticipant participant : participants) {
            participant.setWorkflow(workflow);
            workflow.getParticipants().add(participant);
        }
        return workflow;
    }

    private static WorkflowParticipant userApprover(Long userId, ParticipantSlotStatus status) {
        return WorkflowParticipant.builder()
                .tenantId(TENANT_ID)
                .role(ParticipantRole.APPROVER)
                .requirement(ParticipantRequirement.MANDATORY)
                .principalType(PrincipalType.USER)
                .principalId(userId)
                .slotKey("user:" + userId)
                .slotStatus(status)
                .build();
    }

    @Test
    void openedDoesNotNotifyApproversBecauseTodosHandleThat() {
        Workflow workflow =
                workflowWithParticipants(userApprover(APPROVER_ID, ParticipantSlotStatus.PENDING));

        adapter.onEvent(workflow, WorkflowLifecycleEvent.OPENED, WorkflowEventContext.empty());

        verify(notificationService, never()).notify(any());
    }

    @Test
    void cancelledNotifiesCreatorWithWorkflowCancelledType() {
        Workflow workflow = workflowWithParticipants();
        User creator =
                User.builder()
                        .id(CREATOR_ID)
                        .firstName("Open")
                        .lastName("Er")
                        .username("opener")
                        .build();
        when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.of(creator));

        adapter.onEvent(workflow, WorkflowLifecycleEvent.CANCELLED, WorkflowEventContext.empty());

        ArgumentCaptor<NotificationRequest> captor =
                ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).notify(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.WORKFLOW_CANCELLED);
        assertThat(captor.getValue().recipientUserIds()).containsExactly(CREATOR_ID);
    }

    private static Workflow workflowWithParticipants() {
        return workflowWithParticipants(userApprover(APPROVER_ID, ParticipantSlotStatus.PENDING));
    }

    @Test
    void approvedSkipsNotificationForPurelyAutoApprovedWorkflow() {
        Workflow workflow =
                workflowWithParticipants(userApprover(APPROVER_ID, ParticipantSlotStatus.APPROVED));
        workflow.getDecisions()
                .add(
                        WorkflowDecision.builder()
                                .tenantId(TENANT_ID)
                                .workflow(workflow)
                                .participantId(1L)
                                .userId(null)
                                .decisionType(DecisionType.APPROVED)
                                .decidedAt(Instant.now())
                                .build());

        adapter.onEvent(workflow, WorkflowLifecycleEvent.APPROVED, WorkflowEventContext.empty());

        verify(notificationService, never()).notify(any());
    }

    @Test
    void approvedNotifiesCreatorWithSemanticDecisionParams() {
        Workflow workflow =
                workflowWithParticipants(userApprover(APPROVER_ID, ParticipantSlotStatus.APPROVED));
        workflow.getDecisions()
                .add(
                        WorkflowDecision.builder()
                                .tenantId(TENANT_ID)
                                .workflow(workflow)
                                .participantId(1L)
                                .userId(APPROVER_ID)
                                .decisionType(DecisionType.APPROVED)
                                .comment("Sieht gut aus")
                                .decidedAt(Instant.now())
                                .build());
        when(userRepository.findById(CREATOR_ID))
                .thenReturn(
                        java.util.Optional.of(
                                User.builder()
                                        .id(CREATOR_ID)
                                        .firstName("Max")
                                        .lastName("Mustermann")
                                        .build()));
        when(userRepository.findById(APPROVER_ID))
                .thenReturn(
                        java.util.Optional.of(
                                User.builder()
                                        .id(APPROVER_ID)
                                        .firstName("Anna")
                                        .lastName("Approver")
                                        .build()));

        adapter.onEvent(workflow, WorkflowLifecycleEvent.APPROVED, WorkflowEventContext.empty());

        NotificationRequest request = captureNotify();
        assertThat(request.recipientUserIds()).containsExactly(CREATOR_ID);
        assertThat(request.type()).isEqualTo(NotificationType.WORKFLOW_APPROVED);
        assertThat(request.link()).isEqualTo(AppLink.workflow(WORKFLOW_ID));
        assertThat(request.params())
                .containsEntry(
                        NotificationParamKeys.COMMENTS, List.of("Anna Approver: Sieht gut aus"));
        assertThat(request.params())
                .containsEntry(
                        NotificationParamKeys.DECIDED_BY_DISPLAY_NAMES, List.of("Anna Approver"));
        assertThat(request.params()).doesNotContainKey(NotificationParamKeys.COMMENT_LINE);
        assertThat(request.params())
                .doesNotContainKey(NotificationParamKeys.DECIDED_BY_DISPLAY_NAME);
        assertThat(request.params()).doesNotContainKey(NotificationParamKeys.APPROVER_SUFFIX);
    }

    @Test
    void rejectedNotifiesCreatorWithSemanticComment() {
        Workflow workflow =
                workflowWithParticipants(userApprover(APPROVER_ID, ParticipantSlotStatus.REJECTED));
        workflow.getDecisions()
                .add(
                        WorkflowDecision.builder()
                                .tenantId(TENANT_ID)
                                .workflow(workflow)
                                .participantId(1L)
                                .userId(APPROVER_ID)
                                .decisionType(DecisionType.REJECTED)
                                .comment("Unvollständig")
                                .decidedAt(Instant.now())
                                .build());
        when(userRepository.findById(CREATOR_ID))
                .thenReturn(
                        java.util.Optional.of(
                                User.builder()
                                        .id(CREATOR_ID)
                                        .firstName("Max")
                                        .lastName("Mustermann")
                                        .build()));
        when(userRepository.findById(APPROVER_ID))
                .thenReturn(
                        java.util.Optional.of(
                                User.builder()
                                        .id(APPROVER_ID)
                                        .firstName("Anna")
                                        .lastName("Approver")
                                        .build()));

        adapter.onEvent(workflow, WorkflowLifecycleEvent.REJECTED, WorkflowEventContext.empty());

        NotificationRequest request = captureNotify();
        assertThat(request.recipientUserIds()).containsExactly(CREATOR_ID);
        assertThat(request.type()).isEqualTo(NotificationType.WORKFLOW_REJECTED);
        assertThat(request.params())
                .containsEntry(
                        NotificationParamKeys.COMMENTS, List.of("Anna Approver: Unvollständig"));
        assertThat(request.params()).doesNotContainKey(NotificationParamKeys.COMMENT_LINE);
    }

    @Test
    void approvedListsAllHumanCommentsChronologically() {
        Workflow workflow =
                workflowWithParticipants(userApprover(APPROVER_ID, ParticipantSlotStatus.APPROVED));
        workflow.getDecisions()
                .add(
                        WorkflowDecision.builder()
                                .tenantId(TENANT_ID)
                                .workflow(workflow)
                                .participantId(1L)
                                .userId(APPROVER_ID)
                                .decisionType(DecisionType.APPROVED)
                                .comment("Erster Kommentar")
                                .decidedAt(Instant.now().minus(Duration.ofMinutes(2)))
                                .build());
        workflow.getDecisions()
                .add(
                        WorkflowDecision.builder()
                                .tenantId(TENANT_ID)
                                .workflow(workflow)
                                .participantId(2L)
                                .userId(50L)
                                .decisionType(DecisionType.APPROVED)
                                .comment("Zweiter Kommentar")
                                .decidedAt(Instant.now().minus(Duration.ofMinutes(1)))
                                .build());
        when(userRepository.findById(CREATOR_ID))
                .thenReturn(
                        java.util.Optional.of(
                                User.builder()
                                        .id(CREATOR_ID)
                                        .firstName("Max")
                                        .lastName("Mustermann")
                                        .build()));
        when(userRepository.findById(APPROVER_ID))
                .thenReturn(
                        java.util.Optional.of(
                                User.builder()
                                        .id(APPROVER_ID)
                                        .firstName("Anna")
                                        .lastName("Approver")
                                        .build()));
        when(userRepository.findById(50L))
                .thenReturn(
                        java.util.Optional.of(
                                User.builder()
                                        .id(50L)
                                        .firstName("Ben")
                                        .lastName("Approver")
                                        .build()));

        adapter.onEvent(workflow, WorkflowLifecycleEvent.APPROVED, WorkflowEventContext.empty());

        NotificationRequest request = captureNotify();
        assertThat(request.params())
                .containsEntry(
                        NotificationParamKeys.COMMENTS,
                        List.of(
                                "Anna Approver: Erster Kommentar",
                                "Ben Approver: Zweiter Kommentar"));
    }

    @Test
    void approvedListsAllApproversWhenMultiplePeopleDecided() {
        Workflow workflow =
                workflowWithParticipants(userApprover(APPROVER_ID, ParticipantSlotStatus.APPROVED));
        workflow.getDecisions()
                .add(
                        WorkflowDecision.builder()
                                .tenantId(TENANT_ID)
                                .workflow(workflow)
                                .participantId(1L)
                                .userId(APPROVER_ID)
                                .decisionType(DecisionType.APPROVED)
                                .decidedAt(Instant.now().minus(Duration.ofMinutes(2)))
                                .build());
        workflow.getDecisions()
                .add(
                        WorkflowDecision.builder()
                                .tenantId(TENANT_ID)
                                .workflow(workflow)
                                .participantId(2L)
                                .userId(50L)
                                .decisionType(DecisionType.APPROVED)
                                .decidedAt(Instant.now().minus(Duration.ofMinutes(1)))
                                .build());
        when(userRepository.findById(CREATOR_ID))
                .thenReturn(
                        java.util.Optional.of(
                                User.builder()
                                        .id(CREATOR_ID)
                                        .firstName("Max")
                                        .lastName("Mustermann")
                                        .build()));
        when(userRepository.findById(APPROVER_ID))
                .thenReturn(
                        java.util.Optional.of(
                                User.builder()
                                        .id(APPROVER_ID)
                                        .firstName("Anna")
                                        .lastName("Approver")
                                        .build()));
        when(userRepository.findById(50L))
                .thenReturn(
                        java.util.Optional.of(
                                User.builder()
                                        .id(50L)
                                        .firstName("Ben")
                                        .lastName("Approver")
                                        .build()));

        adapter.onEvent(workflow, WorkflowLifecycleEvent.APPROVED, WorkflowEventContext.empty());

        NotificationRequest request = captureNotify();
        assertThat(request.params())
                .containsEntry(
                        NotificationParamKeys.DECIDED_BY_DISPLAY_NAMES,
                        List.of("Anna Approver", "Ben Approver"));
        assertThat(request.params())
                .doesNotContainKey(NotificationParamKeys.DECIDED_BY_DISPLAY_NAME);
    }

    @Test
    void cancelledNotifiesCreatorOnlyNotApprovers() {
        Workflow workflow =
                workflowWithParticipants(
                        userApprover(APPROVER_ID, ParticipantSlotStatus.PENDING),
                        userApprover(CREATOR_ID, ParticipantSlotStatus.PENDING));
        when(userRepository.findById(CREATOR_ID))
                .thenReturn(
                        Optional.of(
                                User.builder()
                                        .id(CREATOR_ID)
                                        .firstName("Open")
                                        .lastName("Er")
                                        .build()));

        adapter.onEvent(workflow, WorkflowLifecycleEvent.CANCELLED, WorkflowEventContext.empty());

        ArgumentCaptor<NotificationRequest> captor =
                ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).notify(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.WORKFLOW_CANCELLED);
        assertThat(captor.getValue().recipientUserIds()).containsExactly(CREATOR_ID);
    }

    @Test
    void cancelledByCreatorDoesNotNotifyCreator() {
        Workflow workflow = workflowWithParticipants();

        adapter.onEvent(
                workflow,
                WorkflowLifecycleEvent.CANCELLED,
                WorkflowEventContext.ofActor(null, CREATOR_ID));

        verify(notificationService, never()).notify(any());
    }

    @Test
    void cancelledSkipsDeactivatedCreator() {
        Workflow workflow = workflowWithParticipants();
        when(userRepository.findById(CREATOR_ID))
                .thenReturn(
                        Optional.of(
                                User.builder()
                                        .id(CREATOR_ID)
                                        .deactivatedAt(Instant.now())
                                        .build()));

        adapter.onEvent(workflow, WorkflowLifecycleEvent.CANCELLED, WorkflowEventContext.empty());

        verify(notificationService, never()).notify(any());
    }

    @Test
    void inputChangeRequestedNotifiesCreatorWithMessage() {
        Workflow workflow =
                workflowWithParticipants(userApprover(APPROVER_ID, ParticipantSlotStatus.PENDING));
        workflow.getInputChanges()
                .add(
                        WorkflowInputChange.builder()
                                .tenantId(TENANT_ID)
                                .workflow(workflow)
                                .requestedByUserId(APPROVER_ID)
                                .message("Bitte Nachweise ergänzen")
                                .status(WorkflowInputChangeStatus.OPEN)
                                .build());
        when(userRepository.findById(CREATOR_ID))
                .thenReturn(
                        java.util.Optional.of(
                                User.builder()
                                        .id(CREATOR_ID)
                                        .firstName("Max")
                                        .lastName("Mustermann")
                                        .build()));

        adapter.onEvent(
                workflow,
                WorkflowLifecycleEvent.INPUT_CHANGE_REQUESTED,
                WorkflowEventContext.ofActor(null, APPROVER_ID));

        NotificationRequest request = captureNotify();
        assertThat(request.type()).isEqualTo(NotificationType.WORKFLOW_INPUT_CHANGE_REQUESTED);
        assertThat(request.recipientUserIds()).containsExactly(CREATOR_ID);
        assertThat(request.params())
                .containsEntry(
                        NotificationParamKeys.INPUT_CHANGE_MESSAGE, "Bitte Nachweise ergänzen");
        assertThat(request.link()).isEqualTo(AppLink.workflow(WORKFLOW_ID));
    }

    @Test
    void inputChangeRequestedSkipsNotificationWhenCreatorRequestsOwnChange() {
        Workflow workflow =
                workflowWithParticipants(userApprover(CREATOR_ID, ParticipantSlotStatus.PENDING));
        workflow.getInputChanges()
                .add(
                        WorkflowInputChange.builder()
                                .tenantId(TENANT_ID)
                                .workflow(workflow)
                                .requestedByUserId(CREATOR_ID)
                                .message("Selbstkorrektur")
                                .status(WorkflowInputChangeStatus.OPEN)
                                .build());

        adapter.onEvent(
                workflow,
                WorkflowLifecycleEvent.INPUT_CHANGE_REQUESTED,
                WorkflowEventContext.ofActor(null, CREATOR_ID));

        verify(notificationService, never()).notify(any());
    }

    private NotificationRequest captureNotify() {
        ArgumentCaptor<NotificationRequest> captor =
                ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).notify(captor.capture());
        return captor.getValue();
    }
}
