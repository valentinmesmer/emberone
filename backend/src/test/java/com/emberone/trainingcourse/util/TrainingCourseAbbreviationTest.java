/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.platform.exception.ConflictException;
import org.junit.jupiter.api.Test;

class TrainingCourseAbbreviationTest {

    @Test
    void normalizeUppercasesAndCollapsesWhitespace() {
        assertThat(TrainingCourseAbbreviation.normalize("  gf ")).isEqualTo("GF");
        assertThat(TrainingCourseAbbreviation.normalize("mta  zp")).isEqualTo("MTA ZP");
    }

    @Test
    void requireValidAcceptsTypicalAbbreviations() {
        assertThat(TrainingCourseAbbreviation.requireValid("GF")).isEqualTo("GF");
        assertThat(TrainingCourseAbbreviation.requireValid("MTA ZP")).isEqualTo("MTA ZP");
        assertThat(TrainingCourseAbbreviation.requireValid("FROM2")).isEqualTo("FROM2");
    }

    @Test
    void requireValidRejectsBlank() {
        assertThatThrownBy(() -> TrainingCourseAbbreviation.requireValid("  "))
                .isInstanceOf(ConflictException.class);
    }
}
