/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.exception;

import com.emberone.platform.exception.ConflictException;

public class RetentionConfigurationException extends ConflictException {

    public RetentionConfigurationException(String message) {
        super(message);
    }
}
