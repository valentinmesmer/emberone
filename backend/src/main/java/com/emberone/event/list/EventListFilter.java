/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.list;

import com.emberone.platform.web.list.ListFilters;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Organization service/operation event list filters. {@code from}/{@code to} start-time window;
 * when set, {@code year} and {@code attendance} are still applied.
 */
public record EventListFilter(
        Instant from, Instant to, List<Integer> years, List<EventListAttendanceStatus> attendance) {

    public EventListFilter {
        if (years == null) {
            years = List.of();
        }
        if (attendance == null) {
            attendance = List.of();
        }
    }

    public boolean hasDateRange() {
        return from != null || to != null;
    }

    public static EventListFilter forDateRange(Instant from, Instant to) {
        return new EventListFilter(from, to, List.of(), List.of());
    }

    public static EventListFilter parseOrg(List<Integer> year, List<String> attendance) {
        return new EventListFilter(
                null,
                null,
                year == null ? List.of() : List.copyOf(year),
                ListFilters.parseEnumList(
                        attendance,
                        "attendance",
                        raw -> {
                            try {
                                return Optional.of(EventListAttendanceStatus.valueOf(raw));
                            } catch (IllegalArgumentException ex) {
                                return Optional.empty();
                            }
                        }));
    }
}
