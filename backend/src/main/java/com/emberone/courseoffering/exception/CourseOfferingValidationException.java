/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.exception;

import com.emberone.platform.exception.ValidationException;

public class CourseOfferingValidationException extends ValidationException {
    public CourseOfferingValidationException(String message) {
        super(message);
    }
}
