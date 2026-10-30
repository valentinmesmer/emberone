/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class CourseOfferingNotFoundException extends ResourceNotFoundException {
    public CourseOfferingNotFoundException(Long offeringId) {
        super("Course offering not found: " + offeringId);
    }
}
