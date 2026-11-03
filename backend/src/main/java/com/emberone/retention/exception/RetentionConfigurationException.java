/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.retention.exception;

import com.emberone.platform.exception.ConflictException;

public class RetentionConfigurationException extends ConflictException {

    public RetentionConfigurationException(String message) {
        super(message);
    }
}
