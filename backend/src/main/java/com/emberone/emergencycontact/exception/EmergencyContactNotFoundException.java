/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.emergencycontact.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class EmergencyContactNotFoundException extends ResourceNotFoundException {

    public EmergencyContactNotFoundException(Long contactId) {
        super("Emergency contact not found: " + contactId);
    }
}
