/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.exception;

import com.emberone.platform.exception.ConflictException;

/**
 * Raised when organization creation is attempted before a custom standard is cloned and assigned
 * for the target organization type.
 */
public class StandardNotConfiguredException extends ConflictException {

    public static final String MESSAGE =
            "Clone the system standard and assign it to the organization type before creating organizations";

    public StandardNotConfiguredException() {
        super(MESSAGE);
    }
}
