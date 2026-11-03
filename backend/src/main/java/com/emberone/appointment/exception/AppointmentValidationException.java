/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.appointment.exception;

import com.emberone.platform.exception.ValidationException;

public class AppointmentValidationException extends ValidationException {
    public AppointmentValidationException(String message) {
        super(message);
    }
}
