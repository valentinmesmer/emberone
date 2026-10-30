/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.list;

import com.emberone.platform.web.list.ListFilters;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Organization appointment list filters. {@code from}/{@code to} overlap window (import lookups);
 * when set, {@code period} and other list filters are ignored. Empty {@code period} means no
 * upcoming/past restriction.
 */
public record AppointmentListFilter(
        Instant from,
        Instant to,
        List<AppointmentPeriod> periods,
        List<Integer> years,
        List<AppointmentListViewedStatus> viewed,
        List<AppointmentListResponseStatus> response) {

    public AppointmentListFilter {
        if (periods == null) {
            periods = List.of();
        }
        if (years == null) {
            years = List.of();
        }
        if (viewed == null) {
            viewed = List.of();
        }
        if (response == null) {
            response = List.of();
        }
    }

    public boolean hasDateRange() {
        return from != null || to != null;
    }

    public static AppointmentListFilter forDateRange(Instant from, Instant to) {
        return new AppointmentListFilter(from, to, List.of(), List.of(), List.of(), List.of());
    }

    public static AppointmentListFilter parseOrg(
            List<String> period, List<Integer> year, List<String> viewed, List<String> response) {
        return new AppointmentListFilter(
                null,
                null,
                ListFilters.parseEnumList(
                        period,
                        "period",
                        raw -> {
                            try {
                                return Optional.of(AppointmentPeriod.valueOf(raw));
                            } catch (IllegalArgumentException ex) {
                                return Optional.empty();
                            }
                        }),
                year == null ? List.of() : List.copyOf(year),
                ListFilters.parseEnumList(
                        viewed,
                        "viewed",
                        raw -> {
                            try {
                                return Optional.of(AppointmentListViewedStatus.valueOf(raw));
                            } catch (IllegalArgumentException ex) {
                                return Optional.empty();
                            }
                        }),
                ListFilters.parseEnumList(
                        response,
                        "response",
                        raw -> {
                            try {
                                return Optional.of(AppointmentListResponseStatus.valueOf(raw));
                            } catch (IllegalArgumentException ex) {
                                return Optional.empty();
                            }
                        }));
    }

    /**
     * Sort override when exactly one period bucket is selected; otherwise default upcoming order.
     */
    public String sortOverride() {
        if (hasDateRange()) {
            return null;
        }
        if (periods.size() == 1) {
            return periods.get(0).sort();
        }
        return AppointmentPeriod.UPCOMING.sort();
    }
}
