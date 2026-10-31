/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.emergencycontact.exception;

import com.emberone.platform.exception.ValidationException;

public class InvalidEmergencyContactOrderException extends ValidationException {

    public InvalidEmergencyContactOrderException(String message) {
        super(message);
    }
}
