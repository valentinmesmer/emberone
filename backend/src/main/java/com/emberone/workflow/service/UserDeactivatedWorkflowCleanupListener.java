/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.user.event.UserDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/** Cancels active workflows opened by a user when they are deactivated. */
@Service
@Order(1)
@RequiredArgsConstructor
public class UserDeactivatedWorkflowCleanupListener {

    private final WorkflowService workflowService;

    @EventListener
    public void onUserDeactivated(UserDeactivatedEvent event) {
        workflowService.cancelActiveWorkflowsOpenedByUser(event.userId());
        workflowService.cancelActiveWorkflowsForSubjectUser(event.userId());
    }
}
