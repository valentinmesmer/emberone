/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.domain;

/** Lifecycle of one user's registration for a course offering. */
public enum CourseRegistrationStatus {
    /** The registration workflow is open; no seat or waitlist place yet. */
    PENDING,
    /** Approved and holding one of the offering's seats. */
    PARTICIPANT,
    /** Approved but the offering was full; waits for a manual move-up by the organization. */
    WAITLIST,
    /** The registration workflow was rejected. */
    REJECTED,
    /** Ended after being opened: withdrawn by the user or removed by the organization. */
    CANCELLED;

    /** Registration still occupies something (open request, seat or waitlist place). */
    public boolean isActive() {
        return this == PENDING || this == PARTICIPANT || this == WAITLIST;
    }
}
