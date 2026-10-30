/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.event;

/** Published when the offering organization removed a participant or waitlist place. */
public record CourseRegistrationRemovedEvent(
        Long tenantId,
        Long userId,
        Long offeringId,
        String trainingCourseTypeName,
        String reason) {}
