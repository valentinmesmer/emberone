/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.list;

/** The caller's own registration on an offering; a selection is matched as a union. */
public enum SelfCourseOfferingParticipationState {
    /** Pending registration request. */
    REQUESTED,
    /** The caller is a participant. */
    PARTICIPANT,
    /** The caller is on the waitlist. */
    WAITLIST
}
