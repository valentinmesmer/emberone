/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.list;

import com.emberone.trainingcourse.domain.TrainingCourseUserSubmission;
import org.springframework.data.jpa.domain.Specification;

public final class TrainingCourseRecordListSpecs {

    private TrainingCourseRecordListSpecs() {}

    public static Specification<TrainingCourseUserSubmission> forUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }
}
