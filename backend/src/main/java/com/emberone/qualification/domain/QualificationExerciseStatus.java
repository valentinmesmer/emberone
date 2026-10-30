/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.domain;

public enum QualificationExerciseStatus {
    NOT_AVAILABLE,
    PREREQUISITES_MISSING,
    ORG_DECISION_DENIED,
    ORG_DECISION_REVOKED,
    CAN_EXERCISE
}
