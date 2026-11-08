/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import com.emberone.platform.exception.ConflictException;

/**
 * An AI function is not available: switched off, or its data class exceeds the endpoint trust.
 * Mapped to 409 by the core exception handler.
 */
public class AiUnavailableException extends ConflictException {

    public AiUnavailableException(String message) {
        super(message);
    }
}
