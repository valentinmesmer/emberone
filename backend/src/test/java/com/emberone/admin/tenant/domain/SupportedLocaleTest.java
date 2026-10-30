/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class SupportedLocaleTest {

    @Test
    void fromCode_defaultsEmptyValuesAndNormalizesKnownCodes() {
        assertThat(SupportedLocale.fromCode(null)).isEqualTo(SupportedLocale.DE);
        assertThat(SupportedLocale.fromCode("  ")).isEqualTo(SupportedLocale.DE);
        assertThat(SupportedLocale.fromCode(" DE ")).isEqualTo(SupportedLocale.DE);
    }

    @Test
    void exposesJsonCodeAndJavaLocale() {
        assertThat(SupportedLocale.DE.getCode()).isEqualTo("de");
        assertThat(SupportedLocale.DE.toJavaLocale()).isEqualTo(Locale.GERMAN);
    }

    @Test
    void fromCode_rejectsUnsupportedLocale() {
        assertThatThrownBy(() -> SupportedLocale.fromCode("en"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported locale: en");
    }
}
