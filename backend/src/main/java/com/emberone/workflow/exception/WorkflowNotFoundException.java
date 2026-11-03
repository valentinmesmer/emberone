/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class WorkflowNotFoundException extends ResourceNotFoundException {
    public WorkflowNotFoundException(Long workflowId) {
        super("Workflow not found: " + workflowId);
    }
}
