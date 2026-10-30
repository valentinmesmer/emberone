/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class UserAbsenceNotFoundException extends ResourceNotFoundException {

    public UserAbsenceNotFoundException(Long absenceId) {
        super("Absence not found: " + absenceId);
    }
}
