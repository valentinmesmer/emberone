/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.emergencycontact.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class EmergencyContactNotFoundException extends ResourceNotFoundException {

    public EmergencyContactNotFoundException(Long contactId) {
        super("Emergency contact not found: " + contactId);
    }
}
