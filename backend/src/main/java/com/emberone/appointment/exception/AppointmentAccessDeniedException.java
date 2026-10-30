/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.exception;

import com.emberone.platform.exception.AccessDeniedException;

public class AppointmentAccessDeniedException extends AccessDeniedException {
    public AppointmentAccessDeniedException(String message) {
        super(message);
    }
}
