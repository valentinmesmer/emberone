/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.appointment.exception;

import com.emberone.platform.exception.AccessDeniedException;

public class AppointmentAccessDeniedException extends AccessDeniedException {
    public AppointmentAccessDeniedException(String message) {
        super(message);
    }
}
