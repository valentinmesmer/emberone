/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.courseoffering.domain;

/** What a prerequisite refers to; the config key names a row of the matching tenant catalog. */
public enum CoursePrerequisiteKind {
    TRAINING_COURSE,
    CERTIFICATION,
    DRIVING_LICENSE
}
