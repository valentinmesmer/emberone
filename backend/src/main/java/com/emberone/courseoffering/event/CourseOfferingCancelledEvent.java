/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.event;

import java.util.Set;

/**
 * The offering organization cancelled a published offering. {@code userIds} are all users whose
 * registration ended with it: participants, waitlist and open applicants.
 */
public record CourseOfferingCancelledEvent(
        Long tenantId,
        Long offeringId,
        String trainingCourseTypeName,
        String reason,
        Set<Long> userIds) {}
