/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.timeentry.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class UserTimeEntryNotFoundException extends ResourceNotFoundException {
    public UserTimeEntryNotFoundException(Long id) {
        super("Time entry not found: " + id);
    }
}
