/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.todo.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class TodoNotFoundException extends ResourceNotFoundException {
    public TodoNotFoundException(Long id) {
        super("Todo not found: " + id);
    }
}
