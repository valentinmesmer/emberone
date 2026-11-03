/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.courseoffering.list;

import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import java.util.List;

/** {@code upcoming=true}: only offerings whose last session has not ended yet. */
public record UserCourseRegistrationListFilter(
        List<CourseRegistrationStatus> statuses, boolean upcomingOnly) {

    public static UserCourseRegistrationListFilter parse(
            List<String> status, List<String> upcoming) {
        return new UserCourseRegistrationListFilter(
                CourseRegistrationListFilter.parse(status).statuses(),
                upcoming != null && upcoming.contains("true"));
    }
}
