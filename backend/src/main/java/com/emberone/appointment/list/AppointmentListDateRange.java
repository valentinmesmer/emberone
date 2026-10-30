/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.list;

import com.emberone.appointment.service.AppointmentRecurrenceService;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

/** Resolves default and validated {@code from}/{@code to} for appointment list queries. */
public final class AppointmentListDateRange {

    public static final Duration MAX_SPAN =
            Duration.ofDays((long) AppointmentRecurrenceService.SERIES_HORIZON_MONTHS * 31L);

    private AppointmentListDateRange() {}

    public static Resolved resolve(Instant from, Instant to) {
        Instant now = Instant.now();
        Instant rangeFrom = from != null ? from : minusMonths(now, 1);
        Instant rangeTo =
                to != null
                        ? to
                        : plusMonths(now, AppointmentRecurrenceService.SERIES_HORIZON_MONTHS);
        if (!rangeTo.isAfter(rangeFrom)) {
            throw new IllegalArgumentException("invalid date range");
        }
        if (from != null || to != null) {
            enforceMaxSpan(rangeFrom, rangeTo);
        }
        return new Resolved(rangeFrom, rangeTo);
    }

    private static void enforceMaxSpan(Instant from, Instant to) {
        if (Duration.between(from, to).compareTo(MAX_SPAN) > 0) {
            throw new IllegalArgumentException(
                    "date range exceeds maximum span of "
                            + AppointmentRecurrenceService.SERIES_HORIZON_MONTHS
                            + " months");
        }
    }

    private static Instant plusMonths(Instant instant, int months) {
        return instant.atZone(ZoneOffset.UTC).plusMonths(months).toInstant();
    }

    private static Instant minusMonths(Instant instant, int months) {
        return instant.atZone(ZoneOffset.UTC).minusMonths(months).toInstant();
    }

    public record Resolved(Instant fromInclusive, Instant toExclusive) {}
}
