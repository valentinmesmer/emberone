/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.validation;

import java.time.LocalDate;

/**
 * Shared date-of-birth rules aligned with the frontend ({@code
 * frontend/lib/validation/dateOfBirth.ts}).
 *
 * <p>Requires a full calendar year in a plausible range: not after today, and not older than {@link
 * ValidationPatterns#DATE_OF_BIRTH_MAX_AGE_YEARS}. This rejects short/incomplete years such as year
 * {@code 4} (often serialized as {@code 0004-…}).
 */
public final class DateOfBirthRules {

    private DateOfBirthRules() {}

    public static boolean isValid(LocalDate date) {
        if (date == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) {
            return false;
        }
        LocalDate earliest = today.minusYears(ValidationPatterns.DATE_OF_BIRTH_MAX_AGE_YEARS);
        return !date.isBefore(earliest);
    }
}
