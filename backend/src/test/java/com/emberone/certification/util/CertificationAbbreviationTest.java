/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.platform.exception.ConflictException;
import org.junit.jupiter.api.Test;

class CertificationAbbreviationTest {

    @Test
    void normalizeTrimsAndCollapsesWhitespace() {
        assertThat(CertificationAbbreviation.normalize("  ABÜ ")).isEqualTo("ABÜ");
        assertThat(CertificationAbbreviation.normalize("G26.3")).isEqualTo("G26.3");
    }

    @Test
    void requireValidAcceptsTypicalAbbreviations() {
        assertThat(CertificationAbbreviation.requireValid("G26.3")).isEqualTo("G26.3");
        assertThat(CertificationAbbreviation.requireValid("ABÜ")).isEqualTo("ABÜ");
        assertThat(CertificationAbbreviation.requireValid("AU")).isEqualTo("AU");
        assertThat(CertificationAbbreviation.requireValid("AÜE")).isEqualTo("AÜE");
    }

    @Test
    void requireValidRejectsBlank() {
        assertThatThrownBy(() -> CertificationAbbreviation.requireValid("  "))
                .isInstanceOf(ConflictException.class);
    }
}
