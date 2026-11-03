/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.user.exception;

import com.emberone.platform.exception.ConflictException;

public class UserAlreadyDeactivatedException extends ConflictException {
    public UserAlreadyDeactivatedException(Long userId) {
        super("User is already deactivated: " + userId);
    }
}
