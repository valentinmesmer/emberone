/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.list;

import com.emberone.platform.web.list.ListFilters;
import java.time.Instant;
import java.util.List;

/**
 * Self lists use {@code organizationId} and {@code year} (calendar year of {@code startTime} in
 * organization wall-clock). Org lists keep optional {@code from}/{@code to} on {@code startTime}.
 */
public record TimeEntryListFilter(
        Instant from,
        Instant to,
        List<Long> organizationIds,
        List<Integer> years,
        boolean selfList) {

    public TimeEntryListFilter {
        if (organizationIds == null) {
            organizationIds = List.of();
        }
        if (years == null) {
            years = List.of();
        }
    }

    public static TimeEntryListFilter forSelf(List<Long> organizationId, List<Integer> year) {
        return new TimeEntryListFilter(
                null,
                null,
                ListFilters.optionalLongIds(organizationId),
                year == null ? List.of() : List.copyOf(year),
                true);
    }

    public static TimeEntryListFilter forRange(Instant from, Instant to) {
        return new TimeEntryListFilter(from, to, List.of(), List.of(), false);
    }

    public Instant resolvedFrom() {
        return selfList ? null : from;
    }

    public Instant resolvedTo() {
        return selfList ? null : to;
    }
}
