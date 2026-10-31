/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.todo.type.workflow;

import com.emberone.todo.domain.TodoType;
import com.emberone.todo.type.AbstractTodoTypeDefinition;
import org.springframework.stereotype.Component;

@Component
public class WorkflowApprovalRequiredTodoDefinition extends AbstractTodoTypeDefinition {

    public WorkflowApprovalRequiredTodoDefinition() {
        // notifiesOnCreate: USER-principal assignees get TODO_ASSIGNED; claim/restore paths
        // set TodoOpenRequest.suppressCreateNotification to avoid self-notify after claim.
        super(TodoType.WORKFLOW_APPROVAL_REQUIRED, true, true);
    }
}
