/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.list;

import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.platform.web.list.ListDefinition;

public final class CourseOfferingListDefinition {

    public static final ListDefinition<CourseOffering> INSTANCE =
            ListDefinition.<CourseOffering>forEntity(CourseOffering.class)
                    .searchField("trainingCourseType.name")
                    .searchField("content")
                    .searchField("notes")
                    .textSortKey("trainingCourseType", "trainingCourseType.name")
                    .sortKey("startDate", "firstSessionStart")
                    .defaultSort("startDate,asc")
                    .build();

    private CourseOfferingListDefinition() {}
}
