/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.timeentry.exception;

import com.emberone.platform.exception.ValidationException;

public class TimeEntryValidationException extends ValidationException {
    public TimeEntryValidationException(String message) {
        super(message);
    }
}
