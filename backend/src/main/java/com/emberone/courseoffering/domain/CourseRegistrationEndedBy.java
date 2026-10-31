/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.courseoffering.domain;

/** Who ended a registration that had been opened. */
public enum CourseRegistrationEndedBy {
    /** The user withdrew the registration, cancelled it by workflow or left the waitlist. */
    USER,
    /** The offering organization removed the participant or waitlist place. */
    ORGANIZATION
}
