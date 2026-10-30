/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DateOfBirthRulesTest {

    @Test
    void rejectsNull() {
        assertThat(DateOfBirthRules.isValid(null)).isFalse();
    }

    @Test
    void rejectsIncompleteYearLikeFourInsteadOfTwoThousandFour() {
        assertThat(DateOfBirthRules.isValid(LocalDate.of(4, 5, 10))).isFalse();
        assertThat(DateOfBirthRules.isValid(LocalDate.of(999, 12, 31))).isFalse();
    }

    @Test
    void rejectsFutureDates() {
        assertThat(DateOfBirthRules.isValid(LocalDate.now().plusDays(1))).isFalse();
    }

    @Test
    void rejectsOlderThanMaxAge() {
        LocalDate tooOld =
                LocalDate.now()
                        .minusYears(ValidationPatterns.DATE_OF_BIRTH_MAX_AGE_YEARS)
                        .minusDays(1);
        assertThat(DateOfBirthRules.isValid(tooOld)).isFalse();
    }

    @Test
    void acceptsPlausibleFourDigitYears() {
        assertThat(DateOfBirthRules.isValid(LocalDate.of(2004, 5, 10))).isTrue();
        assertThat(DateOfBirthRules.isValid(LocalDate.now())).isTrue();
        assertThat(
                        DateOfBirthRules.isValid(
                                LocalDate.now()
                                        .minusYears(
                                                ValidationPatterns.DATE_OF_BIRTH_MAX_AGE_YEARS)))
                .isTrue();
    }
}
