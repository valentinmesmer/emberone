/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.courseoffering.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class CourseRegistrationNotFoundException extends ResourceNotFoundException {
    public CourseRegistrationNotFoundException(Long registrationId) {
        super("Course registration not found: " + registrationId);
    }
}
