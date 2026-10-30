/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.list;

import com.emberone.courseoffering.domain.CourseOfferingStatus;
import com.emberone.platform.web.list.ListFilters;
import java.util.List;
import java.util.Optional;

public record CourseOfferingListFilter(
        List<CourseOfferingStatus> statuses, List<Long> trainingCourseTypeIds) {

    public CourseOfferingListFilter {
        statuses = statuses == null ? List.of() : List.copyOf(statuses);
        trainingCourseTypeIds =
                trainingCourseTypeIds == null ? List.of() : List.copyOf(trainingCourseTypeIds);
    }

    public static CourseOfferingListFilter parse(
            List<String> status, List<String> trainingCourseTypeId) {
        return new CourseOfferingListFilter(
                ListFilters.parseEnumList(
                        status,
                        "status",
                        raw -> {
                            try {
                                return Optional.of(CourseOfferingStatus.valueOf(raw))
                                        .filter(parsed -> parsed != CourseOfferingStatus.TEMPLATE);
                            } catch (IllegalArgumentException ex) {
                                return Optional.empty();
                            }
                        }),
                ListFilters.parseLongList(trainingCourseTypeId, "trainingCourseTypeId"));
    }
}
