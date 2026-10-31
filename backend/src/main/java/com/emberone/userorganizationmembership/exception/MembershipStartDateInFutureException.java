/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userorganizationmembership.exception;

import com.emberone.platform.exception.ValidationException;

public class MembershipStartDateInFutureException extends ValidationException {

    public MembershipStartDateInFutureException() {
        super("Start date cannot be in the future");
    }
}
