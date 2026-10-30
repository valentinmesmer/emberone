/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.type.workflow;

import com.emberone.todo.domain.TodoType;
import com.emberone.todo.type.AbstractTodoTypeDefinition;
import org.springframework.stereotype.Component;

@Component
public class WorkflowInputChangeRequiredTodoDefinition extends AbstractTodoTypeDefinition {

    public WorkflowInputChangeRequiredTodoDefinition() {
        // Creator is notified via WORKFLOW_INPUT_CHANGE_REQUESTED (same pattern as approval todos).
        super(TodoType.WORKFLOW_INPUT_CHANGE_REQUIRED, true, false);
    }
}
