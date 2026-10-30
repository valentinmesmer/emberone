/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.list;

import com.emberone.courseoffering.domain.CourseRegistration;
import com.emberone.platform.web.list.ListDefinition;

/** Registrations of one member across all offerings. */
public final class UserCourseRegistrationListDefinition {

    public static final ListDefinition<CourseRegistration> INSTANCE =
            ListDefinition.<CourseRegistration>forEntity(CourseRegistration.class)
                    .searchField("offering.trainingCourseType.name")
                    .sortKey("startDate", "offering.firstSessionStart")
                    .sortKey("registeredAt", "registeredAt")
                    .textSortKey("courseType", "offering.trainingCourseType.name")
                    .defaultSort("startDate,desc")
                    .build();

    private UserCourseRegistrationListDefinition() {}
}
