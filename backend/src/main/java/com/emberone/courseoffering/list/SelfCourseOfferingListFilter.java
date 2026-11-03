/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.courseoffering.list;

import java.util.Set;

/**
 * {@code includeHeld}: also list offerings for courses the user already holds. {@code
 * includeUnfulfilled}: also list offerings whose prerequisites the user does not meet.
 */
public record SelfCourseOfferingListFilter(
        boolean includeHeld,
        boolean includeUnfulfilled,
        Set<SelfCourseOfferingRegistrationState> registration,
        Set<SelfCourseOfferingParticipationState> participation) {

    public SelfCourseOfferingListFilter(boolean includeHeld, boolean includeUnfulfilled) {
        this(includeHeld, includeUnfulfilled, Set.of(), Set.of());
    }

    public SelfCourseOfferingListFilter(boolean includeHeld) {
        this(includeHeld, false, Set.of(), Set.of());
    }
}
