/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.list;

import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.platform.web.list.ListDefinition;

public final class CourseOfferingTemplateListDefinition {

    public static final ListDefinition<CourseOffering> INSTANCE =
            ListDefinition.<CourseOffering>forEntity(CourseOffering.class)
                    .searchField("templateName")
                    .searchField("trainingCourseType.name")
                    .textSortKey("name", "templateName")
                    .defaultSort("name,asc")
                    .build();

    private CourseOfferingTemplateListDefinition() {}
}
