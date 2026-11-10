/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.rank.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.platform.exception.ConflictException;
import org.junit.jupiter.api.Test;

class RankAbbreviationTest {

    @Test
    void normalizeTrimsAndCollapsesWhitespace() {
        assertThat(RankAbbreviation.normalize("  ABÜ ")).isEqualTo("ABÜ");
        assertThat(RankAbbreviation.normalize("G26.3")).isEqualTo("G26.3");
    }

    @Test
    void requireValidAcceptsTypicalAbbreviations() {
        assertThat(RankAbbreviation.requireValid("G26.3")).isEqualTo("G26.3");
        assertThat(RankAbbreviation.requireValid("ABÜ")).isEqualTo("ABÜ");
        assertThat(RankAbbreviation.requireValid("AU")).isEqualTo("AU");
        assertThat(RankAbbreviation.requireValid("AÜE")).isEqualTo("AÜE");
    }

    @Test
    void requireValidRejectsBlank() {
        assertThatThrownBy(() -> RankAbbreviation.requireValid("  "))
                .isInstanceOf(ConflictException.class);
    }
}
