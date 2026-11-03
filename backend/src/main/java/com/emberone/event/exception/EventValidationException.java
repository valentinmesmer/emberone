/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.event.exception;

import com.emberone.platform.exception.ValidationException;

public class EventValidationException extends ValidationException {

    public EventValidationException(String message) {
        super(message);
    }
}
