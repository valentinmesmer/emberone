/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.event.exception.EventValidationException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class EventListDateRangeTest {

    @Test
    void rejectsSpanExceedingMaxWhenBoundsExplicit() {
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to = from.plus(EventListDateRange.MAX_SPAN).plus(1, ChronoUnit.DAYS);

        assertThatThrownBy(() -> EventListDateRange.resolve(from, to))
                .isInstanceOf(EventValidationException.class);
    }

    @Test
    void resolvesDefaultsWhenBoundsOmitted() {
        EventListDateRange.Resolved resolved = EventListDateRange.resolve(null, null);

        assertThat(resolved.to()).isAfter(resolved.from());
    }
}
