/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.todo.exception;

import com.emberone.platform.exception.AccessDeniedException;

public class TodoAccessDeniedException extends AccessDeniedException {
    public TodoAccessDeniedException(String message) {
        super(message);
    }
}
