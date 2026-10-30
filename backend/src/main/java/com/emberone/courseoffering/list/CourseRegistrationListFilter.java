/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.list;

import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import com.emberone.platform.web.list.ListFilters;
import java.util.List;
import java.util.Optional;

public record CourseRegistrationListFilter(List<CourseRegistrationStatus> statuses) {

    public CourseRegistrationListFilter {
        statuses = statuses == null ? List.of() : List.copyOf(statuses);
    }

    public static CourseRegistrationListFilter parse(List<String> status) {
        return new CourseRegistrationListFilter(
                ListFilters.parseEnumList(
                        status,
                        "status",
                        raw -> {
                            try {
                                return Optional.of(CourseRegistrationStatus.valueOf(raw));
                            } catch (IllegalArgumentException ex) {
                                return Optional.empty();
                            }
                        }));
    }
}
