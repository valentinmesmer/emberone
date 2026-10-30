/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.exception;

import com.emberone.platform.exception.ValidationException;

public class TimeEntryValidationException extends ValidationException {
    public TimeEntryValidationException(String message) {
        super(message);
    }
}
