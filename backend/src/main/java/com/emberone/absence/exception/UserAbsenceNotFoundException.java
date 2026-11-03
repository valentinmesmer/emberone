/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.absence.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class UserAbsenceNotFoundException extends ResourceNotFoundException {

    public UserAbsenceNotFoundException(Long absenceId) {
        super("Absence not found: " + absenceId);
    }
}
