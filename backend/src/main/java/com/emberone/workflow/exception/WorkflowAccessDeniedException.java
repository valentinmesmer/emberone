/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.exception;

import com.emberone.platform.exception.AccessDeniedException;

public class WorkflowAccessDeniedException extends AccessDeniedException {
    public WorkflowAccessDeniedException(String message) {
        super(message);
    }
}
