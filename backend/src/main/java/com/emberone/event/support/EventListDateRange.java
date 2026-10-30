/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.support;

import com.emberone.appointment.service.AppointmentRecurrenceService;
import com.emberone.event.exception.EventValidationException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

/** Resolves and validates time windows for org event list queries. */
public final class EventListDateRange {

    static final Duration MAX_SPAN =
            Duration.ofDays((long) AppointmentRecurrenceService.SERIES_HORIZON_MONTHS * 31L);

    private EventListDateRange() {}

    public static Resolved resolve(Instant from, Instant to) {
        Instant now = Instant.now();
        Instant rangeFrom = from != null ? from : minusMonths(now, 1);
        Instant rangeTo =
                to != null
                        ? to
                        : plusMonths(now, AppointmentRecurrenceService.SERIES_HORIZON_MONTHS);
        if (!rangeTo.isAfter(rangeFrom)) {
            throw new EventValidationException("End must be after start");
        }
        if (from != null || to != null) {
            enforceMaxSpan(rangeFrom, rangeTo);
        }
        return new Resolved(rangeFrom, rangeTo);
    }

    private static void enforceMaxSpan(Instant from, Instant to) {
        if (Duration.between(from, to).compareTo(MAX_SPAN) > 0) {
            throw new EventValidationException(
                    "Date range must not exceed "
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

    public record Resolved(Instant from, Instant to) {}
}
