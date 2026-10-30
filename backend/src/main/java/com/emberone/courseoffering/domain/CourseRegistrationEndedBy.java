/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.domain;

/** Who ended a registration that had been opened. */
public enum CourseRegistrationEndedBy {
    /** The user withdrew the registration, cancelled it by workflow or left the waitlist. */
    USER,
    /** The offering organization removed the participant or waitlist place. */
    ORGANIZATION
}
