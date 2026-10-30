/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.adapter.workflow;

import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.service.NotificationRequest;
import com.emberone.notification.service.NotificationService;
import com.emberone.platform.navigation.domain.AppLink;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.workflow.domain.DecisionType;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowDecision;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.event.WorkflowEventContext;
import com.emberone.workflow.event.WorkflowEventListener;
import com.emberone.workflow.event.WorkflowLifecycleEvent;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class WorkflowNotificationAdapter implements WorkflowEventListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

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

    private static boolean hasHumanApprovalDecision(Workflow workflow) {
        return workflow.getDecisions().stream()
                .anyMatch(
                        decision ->
                                decision.getDecisionType() == DecisionType.APPROVED
                                        && decision.getUserId() != null);
    }

    @Override
    public void onEvent(
            Workflow workflow, WorkflowLifecycleEvent event, WorkflowEventContext context) {
        switch (event) {
            case APPROVED -> {
                if (hasHumanApprovalDecision(workflow)) {
                    notifyCreator(workflow, NotificationType.WORKFLOW_APPROVED);
                }
            }
            case REJECTED -> notifyCreator(workflow, NotificationType.WORKFLOW_REJECTED);
            case CANCELLED -> notifyCancelled(workflow, context.actorUserId());
            case INPUT_CHANGE_REQUESTED ->
                    notifyInputChangeRequested(workflow, context.actorUserId());
            case OPENED,
                    CLAIMED,
                    UNCLAIMED,
                    DECISION_RECORDED,
                    APPROVAL_WITHDRAWN,
                    APPROVER_SLOT_REMOVED,
                    INPUT_CHANGE_WITHDRAWN,
                    INPUT_UPDATED,
                    DECISIONS_RESET,
                    UPDATED,
                    COMMENT_CREATED -> {
                // Approvers are notified via todos; only creator-facing events are sent here.
            }
        }
    }

    private void notifyCreator(Workflow workflow, NotificationType type) {
        Long creatorId = workflow.getOpenedByUserId();
        if (creatorId == null) {
            return;
        }
        DecisionType decisionType =
                type == NotificationType.WORKFLOW_REJECTED
                        ? DecisionType.REJECTED
                        : DecisionType.APPROVED;
        notify(workflow, type, Set.of(creatorId), buildParams(workflow, decisionType));
    }

    private void notifyCancelled(Workflow workflow, Long actorUserId) {
        Long creatorId = workflow.getOpenedByUserId();
        if (creatorId == null || creatorId.equals(actorUserId)) {
            return;
        }
        boolean creatorActive =
                userRepository
                        .findById(creatorId)
                        .map(user -> user.getDeactivatedAt() == null)
                        .orElse(false);
        if (!creatorActive) {
            return;
        }
        notifyCreator(workflow, NotificationType.WORKFLOW_CANCELLED);
    }

    private void notifyInputChangeRequested(Workflow workflow, Long actorUserId) {
        Long creatorId = workflow.getOpenedByUserId();
        if (creatorId == null) {
            return;
        }
        // Self-approve: requester is the opener - the input-change todo is enough, no self-ping.
        if (creatorId.equals(actorUserId)) {
            return;
        }
        Map<String, Object> params = buildParams(workflow, null);
        workflow.findOpenInputChange()
                .ifPresent(
                        change -> {
                            if (StringUtils.hasText(change.getMessage())) {
                                params.put(
                                        NotificationParamKeys.INPUT_CHANGE_MESSAGE,
                                        change.getMessage());
                            }
                        });
        notify(
                workflow,
                NotificationType.WORKFLOW_INPUT_CHANGE_REQUESTED,
                Set.of(creatorId),
                params);
    }

    private void notify(
            Workflow workflow,
            NotificationType type,
            Set<Long> recipientUserIds,
            Map<String, Object> params) {
        notificationService.notify(
                NotificationRequest.builder()
                        .tenantId(workflow.getTenantId())
                        .recipientUserIds(recipientUserIds)
                        .type(type)
                        .params(params)
                        .link(AppLink.workflow(workflow.getId()))
                        .build());
    }

    private Map<String, Object> buildParams(Workflow workflow, DecisionType decisionType) {
        Map<String, Object> params = new HashMap<>();
        params.put(NotificationParamKeys.WORKFLOW_ID, workflow.getId());
        params.put(NotificationParamKeys.WORKFLOW_TYPE_KEY, resolveWorkflowTypeKey(workflow));
        params.put(
                NotificationParamKeys.OPENED_BY_DISPLAY_NAME,
                resolveUserDisplayName(workflow.getOpenedByUserId()));

        if (decisionType != null) {
            List<String> decisionDisplayNames =
                    resolveDecisionUserDisplayNames(workflow, decisionType);
            if (!decisionDisplayNames.isEmpty()) {
                params.put(NotificationParamKeys.DECIDED_BY_DISPLAY_NAMES, decisionDisplayNames);
            }
            List<String> comments = collectHumanDecisionComments(workflow, decisionType);
            if (!comments.isEmpty()) {
                params.put(NotificationParamKeys.COMMENTS, comments);
            }
        }
        return params;
    }

    private String resolveUserDisplayName(Long userId) {
        if (userId == null) {
            return "";
        }
        return userRepository
                .findById(userId)
                .map(WorkflowNotificationAdapter::formatUserDisplayName)
                .orElse("");
    }

    private List<String> resolveDecisionUserDisplayNames(
            Workflow workflow, DecisionType decisionType) {
        Set<Long> seenUserIds = new LinkedHashSet<>();
        List<String> names = new ArrayList<>();
        workflow.getDecisions().stream()
                .filter(decision -> decision.getDecisionType() == decisionType)
                .sorted(Comparator.comparing(WorkflowDecision::getDecidedAt))
                .forEach(
                        decision -> {
                            Long userId = decision.getUserId();
                            if (userId == null || !seenUserIds.add(userId)) {
                                return;
                            }
                            String name = resolveUserDisplayName(userId);
                            if (StringUtils.hasText(name)) {
                                names.add(name);
                            }
                        });
        return names;
    }

    private List<String> collectHumanDecisionComments(
            Workflow workflow, DecisionType decisionType) {
        return workflow.getDecisions().stream()
                .filter(decision -> decision.getDecisionType() == decisionType)
                .filter(decision -> decision.getUserId() != null)
                .filter(decision -> StringUtils.hasText(decision.getComment()))
                .sorted(
                        Comparator.comparing(
                                WorkflowDecision::getDecidedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::formatHumanDecisionComment)
                .toList();
    }

    private String formatHumanDecisionComment(WorkflowDecision decision) {
        String comment = decision.getComment().trim();
        String name = resolveUserDisplayName(decision.getUserId());
        if (StringUtils.hasText(name)) {
            return name + ": " + comment;
        }
        return comment;
    }
}
