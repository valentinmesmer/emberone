/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.management.exception;

import com.emberone.platform.exception.ValidationException;

public class ManagementActivationValidationException extends ValidationException {
    public ManagementActivationValidationException(String message) {
        super(message);
    }
}
