/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.event;

import com.emberone.workflow.domain.Workflow;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEventPublisher {

    private final List<WorkflowEventListener> listeners;

    public void publish(Workflow workflow, WorkflowLifecycleEvent event) {
        publish(workflow, event, WorkflowEventContext.empty());
    }

    public void publish(
            Workflow workflow, WorkflowLifecycleEvent event, WorkflowEventContext context) {
        WorkflowEventContext safeContext = context != null ? context : WorkflowEventContext.empty();
        for (WorkflowEventListener listener : listeners) {
            try {
                listener.onEvent(workflow, event, safeContext);
            } catch (Exception e) {
                log.warn(
                        "Workflow event listener {} failed for event {} on workflow {}",
                        listener.getClass().getSimpleName(),
                        event,
                        workflow.getId(),
                        e);
            }
        }
    }
}
