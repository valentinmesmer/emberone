/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.platform.exception.ConflictException;
import org.junit.jupiter.api.Test;

class OnboardingDocumentAbbreviationTest {

    @Test
    void normalizeUppercasesAndCollapsesWhitespace() {
        assertThat(OnboardingDocumentAbbreviation.normalize("  dsgvo ")).isEqualTo("DSGVO");
        assertThat(OnboardingDocumentAbbreviation.normalize("eintritt  2")).isEqualTo("EINTRITT 2");
    }

    @Test
    void requireValidAcceptsTypicalAbbreviations() {
        assertThat(OnboardingDocumentAbbreviation.requireValid("DSGVO")).isEqualTo("DSGVO");
        assertThat(OnboardingDocumentAbbreviation.requireValid("dsgvo")).isEqualTo("DSGVO");
        assertThat(OnboardingDocumentAbbreviation.requireValid("EINTRITT-2"))
                .isEqualTo("EINTRITT-2");
    }

    @Test
    void requireValidRejectsBlank() {
        assertThatThrownBy(() -> OnboardingDocumentAbbreviation.requireValid("  "))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void requireValidRejectsConfigKeyStyleValues() {
        assertThatThrownBy(() -> OnboardingDocumentAbbreviation.requireValid("custom:dsgvo"))
                .isInstanceOf(ConflictException.class);
    }
}
