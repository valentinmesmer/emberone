/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class TodoNotFoundException extends ResourceNotFoundException {
    public TodoNotFoundException(Long id) {
        super("Todo not found: " + id);
    }
}
