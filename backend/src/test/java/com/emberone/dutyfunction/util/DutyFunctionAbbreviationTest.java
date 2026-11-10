/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.dutyfunction.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.platform.exception.ConflictException;
import org.junit.jupiter.api.Test;

class DutyFunctionAbbreviationTest {

    @Test
    void normalizeTrimsAndCollapsesWhitespace() {
        assertThat(DutyFunctionAbbreviation.normalize("  ABÜ ")).isEqualTo("ABÜ");
        assertThat(DutyFunctionAbbreviation.normalize("G26.3")).isEqualTo("G26.3");
    }

    @Test
    void requireValidAcceptsTypicalAbbreviations() {
        assertThat(DutyFunctionAbbreviation.requireValid("G26.3")).isEqualTo("G26.3");
        assertThat(DutyFunctionAbbreviation.requireValid("ABÜ")).isEqualTo("ABÜ");
        assertThat(DutyFunctionAbbreviation.requireValid("AU")).isEqualTo("AU");
        assertThat(DutyFunctionAbbreviation.requireValid("AÜE")).isEqualTo("AÜE");
    }

    @Test
    void requireValidRejectsBlank() {
        assertThatThrownBy(() -> DutyFunctionAbbreviation.requireValid("  "))
                .isInstanceOf(ConflictException.class);
    }
}
