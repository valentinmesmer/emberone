/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrganizationCodeRulesTest {

    @Test
    void acceptsValidCode() {
        assertThat(OrganizationCodeRules.isValid("FW1")).isTrue();
        assertThat(OrganizationCodeRules.isValid("A_B2")).isTrue();
    }

    @Test
    void normalizesCaseAndWhitespace() {
        assertThat(OrganizationCodeRules.isValid(" fw1 ")).isTrue();
    }

    @Test
    void rejectsInvalidCode() {
        assertThat(OrganizationCodeRules.isValid(null)).isFalse();
        assertThat(OrganizationCodeRules.isValid("")).isFalse();
        assertThat(OrganizationCodeRules.isValid("   ")).isFalse();
        assertThat(OrganizationCodeRules.isValid("AB-CD")).isFalse();
        assertThat(OrganizationCodeRules.isValid("TOOLONG")).isFalse();
    }
}
