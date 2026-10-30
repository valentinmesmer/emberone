/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class AppointmentNotFoundException extends ResourceNotFoundException {
    public AppointmentNotFoundException(Long appointmentId) {
        super("Appointment not found: " + appointmentId);
    }
}
