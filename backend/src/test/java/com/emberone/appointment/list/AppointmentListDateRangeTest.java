/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class AppointmentListDateRangeTest {

    @Test
    void resolveRejectsSpanBeyondMaximum() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = from.plus(AppointmentListDateRange.MAX_SPAN).plus(1, ChronoUnit.DAYS);

        assertThatThrownBy(() -> AppointmentListDateRange.resolve(from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum span");
    }

    @Test
    void resolveRejectsEmptyRange() {
        Instant at = Instant.parse("2026-06-01T12:00:00Z");

        assertThatThrownBy(() -> AppointmentListDateRange.resolve(at, at))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid date range");
    }

    @Test
    void resolveAppliesDefaultsWhenBoundsMissing() {
        AppointmentListDateRange.Resolved resolved = AppointmentListDateRange.resolve(null, null);

        assertThat(resolved.toExclusive()).isAfter(resolved.fromInclusive());
    }
}
