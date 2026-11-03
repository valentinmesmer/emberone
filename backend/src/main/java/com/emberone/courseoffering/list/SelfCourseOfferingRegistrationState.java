/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.courseoffering.list;

/** Registration window of an offering; a selection is matched as a union. */
public enum SelfCourseOfferingRegistrationState {
    /** Registration window is open right now. */
    OPEN,
    /** Registration window is not open right now (ended or not yet started). */
    CLOSED,
    /** No seat or quota left. */
    FULL
}
