/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.exception;

import com.emberone.platform.exception.ConflictException;

public class TodoStateException extends ConflictException {
    public TodoStateException(String message) {
        super(message);
    }
}
