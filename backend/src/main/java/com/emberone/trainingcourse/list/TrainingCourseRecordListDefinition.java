/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.list;

import com.emberone.platform.web.list.ListDefinition;
import com.emberone.trainingcourse.domain.TrainingCourseUserSubmission;

public final class TrainingCourseRecordListDefinition {

    public static final ListDefinition<TrainingCourseUserSubmission> INSTANCE =
            ListDefinition.<TrainingCourseUserSubmission>forEntity(
                            TrainingCourseUserSubmission.class)
                    .searchField("name")
                    .sortKey("endDate", "endDate")
                    .textSortKey("name", "name")
                    .defaultSort("endDate,desc")
                    .build();

    private TrainingCourseRecordListDefinition() {}
}
