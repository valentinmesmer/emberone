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
public class WorkflowStartRequestedTodoDefinition extends AbstractTodoTypeDefinition {

    public WorkflowStartRequestedTodoDefinition() {
        super(TodoType.WORKFLOW_START_REQUESTED, true, true);
    }
}
