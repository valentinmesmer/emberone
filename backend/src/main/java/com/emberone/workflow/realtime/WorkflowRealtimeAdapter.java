/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.realtime;

import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.event.WorkflowEventContext;
import com.emberone.workflow.event.WorkflowEventListener;
import com.emberone.workflow.event.WorkflowLifecycleEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Pushes workflow realtime updates; registered as a {@link WorkflowEventListener} so {@code
 * WorkflowService} does not depend on this notifier directly.
 */
@Component
@RequiredArgsConstructor
public class WorkflowRealtimeAdapter implements WorkflowEventListener {

    private final WorkflowRealtimeNotifier notifier;

    @Override
    public void onEvent(
            Workflow workflow, WorkflowLifecycleEvent event, WorkflowEventContext context) {
        switch (event) {
            case UPDATED,
                    OPENED,
                    CLAIMED,
                    UNCLAIMED,
                    DECISION_RECORDED,
                    APPROVAL_WITHDRAWN,
                    APPROVER_SLOT_REMOVED,
                    INPUT_CHANGE_REQUESTED,
                    INPUT_CHANGE_WITHDRAWN,
                    INPUT_UPDATED,
                    DECISIONS_RESET,
                    APPROVED,
                    REJECTED,
                    CANCELLED ->
                    notifier.notifyUpdated(workflow);
            case COMMENT_CREATED -> notifier.notifyCommentCreated(workflow);
        }
    }
}
