/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.event;

import com.emberone.workflow.domain.Workflow;

/**
 * Extensible hook for lifecycle transitions. The notification module registers {@link
 * com.emberone.notification.adapter.workflow.WorkflowNotificationAdapter}; additional listeners can
 * be added without changing {@code WorkflowService}.
 */
public interface WorkflowEventListener {

    void onEvent(Workflow workflow, WorkflowLifecycleEvent event, WorkflowEventContext context);
}
