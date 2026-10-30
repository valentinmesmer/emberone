/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.exception;

/** User would have no organization anchor and no pending onboarding after a membership change. */
public class UserAccountSustainabilityException extends RuntimeException {

    public UserAccountSustainabilityException(String message) {
        super(message);
    }
}
