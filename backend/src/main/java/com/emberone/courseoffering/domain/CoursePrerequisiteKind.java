/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.domain;

/** What a prerequisite refers to; the config key names a row of the matching tenant catalog. */
public enum CoursePrerequisiteKind {
    TRAINING_COURSE,
    CERTIFICATION,
    DRIVING_LICENSE
}
