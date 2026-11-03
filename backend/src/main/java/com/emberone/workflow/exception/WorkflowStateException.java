/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.exception;

import com.emberone.platform.exception.ConflictException;

public class WorkflowStateException extends ConflictException {
    public WorkflowStateException(String message) {
        super(message);
    }
}
