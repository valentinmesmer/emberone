/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.qualification.domain.QualificationExerciseStatus;
import org.junit.jupiter.api.Test;

class QualificationStatusServiceTest {

    @Test
    void withoutOrgDecision_prerequisitesMet_returnsCanExercise() {
        assertThat(QualificationStatusService.resolveStatus(false, true, false, false))
                .isEqualTo(QualificationExerciseStatus.CAN_EXERCISE);
    }

    @Test
    void withoutOrgDecision_prerequisitesMissing_returnsPrerequisitesMissing() {
        assertThat(QualificationStatusService.resolveStatus(false, false, false, false))
                .isEqualTo(QualificationExerciseStatus.PREREQUISITES_MISSING);
    }

    @Test
    void withOrgDecision_noGrant_defaultsToDenied() {
        assertThat(QualificationStatusService.resolveStatus(true, true, false, false))
                .isEqualTo(QualificationExerciseStatus.ORG_DECISION_DENIED);
    }

    @Test
    void withOrgDecision_granted_returnsCanExercise() {
        assertThat(QualificationStatusService.resolveStatus(true, true, true, false))
                .isEqualTo(QualificationExerciseStatus.CAN_EXERCISE);
    }

    @Test
    void withOrgDecision_revoked_returnsRevoked() {
        assertThat(QualificationStatusService.resolveStatus(true, true, false, true))
                .isEqualTo(QualificationExerciseStatus.ORG_DECISION_REVOKED);
    }
}
