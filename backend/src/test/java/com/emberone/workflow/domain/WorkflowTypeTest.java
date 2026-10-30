/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WorkflowTypeTest {

    @Test
    void fromIdResolvesKnownTypes() {
        assertThat(WorkflowType.fromId(1L)).isEqualTo(WorkflowType.NAME_CHANGE);
        assertThat(WorkflowType.fromId(2)).isEqualTo(WorkflowType.ADDRESS_CHANGE);
    }

    @Test
    void fromIdReturnsNullForNullInput() {
        assertThat(WorkflowType.fromId(null)).isNull();
    }

    @Test
    void fromIdThrowsForUnknownId() {
        assertThatThrownBy(() -> WorkflowType.fromId(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown WorkflowType");
    }

    @Test
    void isKnownIdRejectsUnknownId() {
        assertThat(WorkflowType.isKnownId(99L)).isFalse();
    }

    @Test
    void isKnownIdAcceptsActiveWorkflowTypes() {
        assertThat(WorkflowType.isKnownId(WorkflowType.TRAINING_COURSE_RECORD.getId())).isTrue();
        assertThat(WorkflowType.isKnownId(WorkflowType.CERTIFICATION_RECORD.getId())).isTrue();
    }

    @Test
    void getIdReturnsStableDiscriminator() {
        assertThat(WorkflowType.NAME_CHANGE.getId()).isEqualTo(1L);
        assertThat(WorkflowType.ADDRESS_CHANGE.getId()).isEqualTo(2L);
        assertThat(WorkflowType.CERTIFICATION_RECORD.getId()).isEqualTo(3L);
        assertThat(WorkflowType.TRAINING_COURSE_RECORD.getId()).isEqualTo(4L);
    }
}
