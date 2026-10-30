/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class WorkflowNotFoundException extends ResourceNotFoundException {
    public WorkflowNotFoundException(Long workflowId) {
        super("Workflow not found: " + workflowId);
    }
}
