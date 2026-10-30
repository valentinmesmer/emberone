/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.event;

import java.util.Set;

/**
 * The sessions (date, time or location) of a published offering changed. One event per change set;
 * {@code userIds} are the participants and waitlisted users at that moment.
 */
public record CourseOfferingSessionsChangedEvent(
        Long tenantId, Long offeringId, String trainingCourseTypeName, Set<Long> userIds) {}
