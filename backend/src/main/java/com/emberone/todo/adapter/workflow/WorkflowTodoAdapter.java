/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.adapter.workflow;

import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.platform.navigation.domain.AppLink;
import com.emberone.todo.domain.TodoParamKeys;
import com.emberone.todo.domain.TodoSourceEntityType;
import com.emberone.todo.domain.TodoType;
import com.emberone.todo.service.TodoOpenRequest;
import com.emberone.todo.service.TodoService;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.workflow.domain.*;
import com.emberone.workflow.event.WorkflowEventContext;
import com.emberone.workflow.event.WorkflowEventListener;
import com.emberone.workflow.event.WorkflowLifecycleEvent;
import com.emberone.workflow.service.WorkflowSelfApproveService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowTodoAdapter implements WorkflowEventListener {

    private final TodoService todoService;
    private final UserRepository userRepository;
    private final OrganizationGroupRepository organizationGroupRepository;
    private final WorkflowSelfApproveService selfApproveService;

    private static boolean hasPendingApproverSlots(Workflow workflow) {
        return workflow.getParticipants().stream()
                .anyMatch(
                        participant ->
                                participant.isApprover()
                                        && participant.getSlotStatus()
                                                == ParticipantSlotStatus.PENDING);
    }

    private static String resolveWorkflowTypeKey(Workflow workflow) {
        try {
            WorkflowType type = WorkflowType.fromId(workflow.getWorkflowTypeId());
            return type != null ? type.name() : "UNKNOWN";
        } catch (IllegalArgumentException ex) {
            return "UNKNOWN";
        }
    }

    private static String formatUserDisplayName(User user) {
        if (user == null) {
            return "";
        }
        String name =
                ((user.getFirstName() != null ? user.getFirstName() : "")
                                + " "
                                + (user.getLastName() != null ? user.getLastName() : ""))
                        .trim();
        if (!name.isEmpty()) {
            return name;
        }
        return user.getUsername() != null ? user.getUsername().trim() : "";
    }

    @Override
    public void onEvent(
            Workflow workflow, WorkflowLifecycleEvent event, WorkflowEventContext context) {
        switch (event) {
            case OPENED -> onOpened(workflow);
            case APPROVED ->
                    todoService.completeBySource(
                            TodoSourceEntityType.WORKFLOW, workflow.getId(), null);
            case REJECTED, CANCELLED ->
                    todoService.cancelBySource(
                            TodoSourceEntityType.WORKFLOW, workflow.getId(), null);
            case CLAIMED ->
                    onClaimed(
                            workflow, requireParticipant(workflow, context), context.actorUserId());
            case UNCLAIMED ->
                    onUnclaimed(
                            workflow,
                            requireParticipant(workflow, context),
                            context.previousClaimerUserId());
            case DECISION_RECORDED ->
                    onDecision(
                            workflow, requireParticipant(workflow, context), context.actorUserId());
            case APPROVAL_WITHDRAWN ->
                    onApprovalWithdrawn(
                            workflow, requireParticipant(workflow, context), context.actorUserId());
            case APPROVER_SLOT_REMOVED -> onApproverSlotRemoved(workflow, context);
            case INPUT_CHANGE_REQUESTED -> onInputChangeRequested(workflow);
            case INPUT_CHANGE_WITHDRAWN -> onInputChangeWithdrawn(workflow);
            case INPUT_UPDATED -> onInputUpdated(workflow);
            case DECISIONS_RESET -> restoreApproverTodos(workflow);
            case UPDATED, COMMENT_CREATED -> {
                // No todo side-effects.
            }
        }
    }

    private WorkflowParticipant requireParticipant(
            Workflow workflow, WorkflowEventContext context) {
        if (context.participantId() == null) {
            throw new IllegalStateException("Workflow event requires participantId in context");
        }
        return workflow.getParticipants().stream()
                .filter(participant -> context.participantId().equals(participant.getId()))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Participant "
                                                + context.participantId()
                                                + " not found on workflow "
                                                + workflow.getId()));
    }

    private void onClaimed(Workflow workflow, WorkflowParticipant participant, Long claimerUserId) {
        todoService.archiveOpenBySourceParticipant(
                TodoSourceEntityType.WORKFLOW,
                workflow.getId(),
                TodoType.WORKFLOW_START_REQUESTED,
                participant.getId(),
                claimerUserId);
        openApprovalTodo(workflow, participant, claimerUserId, true);
    }

    private void onUnclaimed(
            Workflow workflow, WorkflowParticipant participant, Long previousClaimerUserId) {
        todoService.cancelOpenBySourceParticipant(
                TodoSourceEntityType.WORKFLOW,
                workflow.getId(),
                TodoType.WORKFLOW_APPROVAL_REQUIRED,
                participant.getId(),
                previousClaimerUserId);
        Long openerId = workflow.getOpenedByUserId();
        boolean blocksSelfApprove = openerId != null && !selfApproveService.isAllowed(workflow);
        openGroupClaimTodo(workflow, participant, blocksSelfApprove);
    }

    private void onDecision(
            Workflow workflow, WorkflowParticipant participant, Long deciderUserId) {
        todoService.archiveOpenBySourceParticipant(
                TodoSourceEntityType.WORKFLOW,
                workflow.getId(),
                TodoType.WORKFLOW_APPROVAL_REQUIRED,
                participant.getId(),
                deciderUserId);
    }

    private void onApprovalWithdrawn(
            Workflow workflow, WorkflowParticipant participant, Long userId) {
        openApprovalTodo(workflow, participant, userId, true);
    }

    private void onApproverSlotRemoved(Workflow workflow, WorkflowEventContext context) {
        if (context.participantId() == null) {
            return;
        }
        todoService.cancelOpenTodosForWorkflowParticipant(
                TodoSourceEntityType.WORKFLOW, workflow.getId(), context.participantId(), null);
    }

    private void onInputChangeRequested(Workflow workflow) {
        todoService.cancelOpenBySourceAndType(
                TodoSourceEntityType.WORKFLOW,
                workflow.getId(),
                TodoType.WORKFLOW_APPROVAL_REQUIRED,
                null);
        todoService.cancelOpenBySourceAndType(
                TodoSourceEntityType.WORKFLOW,
                workflow.getId(),
                TodoType.WORKFLOW_START_REQUESTED,
                null);
        Long openerId = workflow.getOpenedByUserId();
        if (openerId == null) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put(TodoParamKeys.WORKFLOW_ID, workflow.getId());
        params.put(TodoParamKeys.WORKFLOW_TYPE_KEY, resolveWorkflowTypeKey(workflow));
        params.put(TodoParamKeys.OPENED_BY_DISPLAY_NAME, resolveUserDisplayName(openerId));
        params.put(TodoParamKeys.OPENED_BY_USER_ID, openerId);
        workflow.findOpenInputChange()
                .ifPresent(
                        change -> {
                            if (change.getMessage() != null) {
                                params.put(TodoParamKeys.INPUT_CHANGE_MESSAGE, change.getMessage());
                            }
                        });
        todoService.reopenInputChangeTodo(
                TodoOpenRequest.builder()
                        .tenantId(workflow.getTenantId())
                        .type(TodoType.WORKFLOW_INPUT_CHANGE_REQUIRED)
                        .assigneeUserId(openerId)
                        .params(params)
                        .link(AppLink.workflow(workflow.getId()))
                        .sourceEntityType(TodoSourceEntityType.WORKFLOW)
                        .sourceEntityId(workflow.getId())
                        .build());
    }

    private void onInputUpdated(Workflow workflow) {
        todoService.completeOpenBySourceAndType(
                TodoSourceEntityType.WORKFLOW,
                workflow.getId(),
                TodoType.WORKFLOW_INPUT_CHANGE_REQUIRED,
                workflow.getOpenedByUserId());
        restoreApproverTodos(workflow);
    }

    private void onInputChangeWithdrawn(Workflow workflow) {
        todoService.cancelOpenBySourceAndType(
                TodoSourceEntityType.WORKFLOW,
                workflow.getId(),
                TodoType.WORKFLOW_INPUT_CHANGE_REQUIRED,
                workflow.getOpenedByUserId());
        restoreApproverTodos(workflow);
    }

    private void restoreApproverTodos(Workflow workflow) {
        if (workflow.getStatus() != WorkflowStatus.OPEN) {
            return;
        }
        onOpened(workflow);
    }

    private void onOpened(Workflow workflow) {
        if (!hasPendingApproverSlots(workflow)) {
            return;
        }
        Long openerId = workflow.getOpenedByUserId();
        boolean blocksSelfApprove = openerId != null && !selfApproveService.isAllowed(workflow);
        for (WorkflowParticipant participant : workflow.getParticipants()) {
            if (!participant.isApprover()
                    || participant.getSlotStatus() != ParticipantSlotStatus.PENDING) {
                continue;
            }
            if (participant.getPrincipalType() == PrincipalType.ORGANIZATION_GROUP) {
                if (participant.getClaimedByUserId() == null) {
                    openGroupClaimTodo(workflow, participant, blocksSelfApprove);
                } else {
                    openApprovalTodo(workflow, participant, participant.getClaimedByUserId(), true);
                }
                continue;
            }
            if (participant.getPrincipalType() == PrincipalType.USER
                    && participant.getPrincipalId() != null) {
                if (blocksSelfApprove && participant.getPrincipalId().equals(openerId)) {
                    continue;
                }
                openApprovalTodo(workflow, participant, participant.getPrincipalId(), false);
            }
        }
    }

    private void openGroupClaimTodo(
            Workflow workflow, WorkflowParticipant participant, boolean selfApproveBlocked) {
        if (participant.getPrincipalId() == null) {
            return;
        }
        Long openerId = workflow.getOpenedByUserId();
        Set<Long> excludedUserIds =
                selfApproveBlocked && openerId != null ? Set.of(openerId) : Set.of();
        todoService.reopenGroupClaimTodo(
                TodoOpenRequest.builder()
                        .tenantId(workflow.getTenantId())
                        .type(TodoType.WORKFLOW_START_REQUESTED)
                        .organizationGroupIds(Set.of(participant.getPrincipalId()))
                        .excludedUserIds(excludedUserIds)
                        .params(buildParams(workflow, participant))
                        .link(AppLink.workflow(workflow.getId()))
                        .sourceEntityType(TodoSourceEntityType.WORKFLOW)
                        .sourceEntityId(workflow.getId())
                        .build());
    }

    private void openApprovalTodo(
            Workflow workflow,
            WorkflowParticipant participant,
            Long assigneeUserId,
            boolean suppressCreateNotification) {
        if (assigneeUserId == null) {
            return;
        }
        todoService.reopenApprovalTodo(
                TodoOpenRequest.builder()
                        .tenantId(workflow.getTenantId())
                        .type(TodoType.WORKFLOW_APPROVAL_REQUIRED)
                        .assigneeUserId(assigneeUserId)
                        .params(buildParams(workflow, participant))
                        .link(AppLink.workflow(workflow.getId()))
                        .sourceEntityType(TodoSourceEntityType.WORKFLOW)
                        .sourceEntityId(workflow.getId())
                        .suppressCreateNotification(suppressCreateNotification)
                        .build());
    }

    private Map<String, Object> buildParams(Workflow workflow, WorkflowParticipant participant) {
        Map<String, Object> params = new HashMap<>();
        params.put(TodoParamKeys.WORKFLOW_ID, workflow.getId());
        params.put(TodoParamKeys.PARTICIPANT_ID, participant.getId());
        params.put(TodoParamKeys.WORKFLOW_TYPE_KEY, resolveWorkflowTypeKey(workflow));
        params.put(
                TodoParamKeys.OPENED_BY_DISPLAY_NAME,
                resolveUserDisplayName(workflow.getOpenedByUserId()));
        if (workflow.getOpenedByUserId() != null) {
            params.put(TodoParamKeys.OPENED_BY_USER_ID, workflow.getOpenedByUserId());
        }
        if (participant.getPrincipalType() == PrincipalType.ORGANIZATION_GROUP
                && participant.getPrincipalId() != null) {
            params.put(TodoParamKeys.ORGANIZATION_GROUP_ID, participant.getPrincipalId());
            resolveOrganizationGroupName(participant.getPrincipalId())
                    .ifPresent(name -> params.put(TodoParamKeys.ORGANIZATION_GROUP_NAME, name));
        }
        return params;
    }

    private java.util.Optional<String> resolveOrganizationGroupName(Long groupId) {
        return organizationGroupRepository.findById(groupId).map(OrganizationGroup::getName);
    }

    private String resolveUserDisplayName(Long userId) {
        if (userId == null) {
            return "";
        }
        return userRepository
                .findById(userId)
                .map(WorkflowTodoAdapter::formatUserDisplayName)
                .orElse("");
    }
}
