/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AppointmentTemporalUtilTest {

    @Test
    void truncateToMinutesClearsSecondsAndNanos() {
        Instant input =
                LocalDateTime.of(2026, 6, 12, 14, 30, 45, 123_456_789).toInstant(ZoneOffset.UTC);

        assertThat(AppointmentTemporalUtil.truncateToMinutes(input))
                .isEqualTo(LocalDateTime.of(2026, 6, 12, 14, 30).toInstant(ZoneOffset.UTC));
    }

    @Test
    void truncateToMinutesReturnsNullForNullInput() {
        assertThat(AppointmentTemporalUtil.truncateToMinutes(null)).isNull();
    }
}
