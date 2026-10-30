/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.profile;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Objects;

/** Retention duration from a reference instant backwards (cutoff = reference minus period). */
public final class RetentionPeriod {

    private final Period period;

    private RetentionPeriod(Period period) {
        this.period = Objects.requireNonNull(period, "period");
    }

    public static RetentionPeriod ofDays(int days) {
        return new RetentionPeriod(Period.ofDays(days));
    }

    public static RetentionPeriod ofMonths(int months) {
        return new RetentionPeriod(Period.ofMonths(months));
    }

    public static RetentionPeriod ofYears(int years) {
        return new RetentionPeriod(Period.ofYears(years));
    }

    public Instant cutoffBefore(Instant reference) {
        return reference.atZone(ZoneOffset.UTC).minus(period).toInstant();
    }

    /** Compact label stored on {@code retention_runs} (no personal data). */
    public String label() {
        if (period.getYears() > 0) {
            return period.getYears() + "y";
        }
        if (period.getMonths() > 0) {
            return period.getMonths() + "m";
        }
        return period.getDays() + "d";
    }

    /** Single non-zero component for API/UI display (factories set exactly one). */
    public int quantity() {
        if (period.getYears() > 0) {
            return period.getYears();
        }
        if (period.getMonths() > 0) {
            return period.getMonths();
        }
        return period.getDays();
    }

    /** Unit matching {@link #quantity()}. */
    public RetentionPeriodUnit unit() {
        if (period.getYears() > 0) {
            return RetentionPeriodUnit.YEARS;
        }
        if (period.getMonths() > 0) {
            return RetentionPeriodUnit.MONTHS;
        }
        return RetentionPeriodUnit.DAYS;
    }

    /**
     * Whether this period is strictly shorter than {@code other} when measured from the same
     * anchor.
     */
    public boolean isStrictlyShorterThan(RetentionPeriod other) {
        Instant anchor = Instant.parse("2020-06-15T12:00:00Z");
        return cutoffBefore(anchor).isAfter(other.cutoffBefore(anchor));
    }

    Period period() {
        return period;
    }
}
