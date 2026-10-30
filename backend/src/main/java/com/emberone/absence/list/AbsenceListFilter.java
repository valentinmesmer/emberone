/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.list;

import com.emberone.absence.domain.AbsenceReason;
import com.emberone.platform.web.list.ListFilters;
import java.util.List;
import java.util.Optional;

/**
 * Self absence list filters. Repeated {@code period} and {@code reason} params are OR-combined
 * within each dimension; empty {@code period} means no time-window restriction (all absences).
 */
public record AbsenceListFilter(
        List<AbsencePeriod> periods,
        List<AbsenceReason> reasons,
        List<Long> organizationIds,
        List<Integer> years) {

    private static final String DEFAULT_SORT = AbsencePeriod.PAST.sort();

    public AbsenceListFilter {
        if (periods == null) {
            periods = List.of();
        }
        if (reasons == null) {
            reasons = List.of();
        }
        if (organizationIds == null) {
            organizationIds = List.of();
        }
        if (years == null) {
            years = List.of();
        }
    }

    public static AbsenceListFilter empty() {
        return new AbsenceListFilter(List.of(), List.of(), List.of(), List.of());
    }

    public static AbsenceListFilter parse(
            List<String> period,
            List<String> reason,
            List<Long> organizationId,
            List<Integer> year) {
        return new AbsenceListFilter(
                ListFilters.parseEnumList(
                        period,
                        "period",
                        raw -> {
                            try {
                                return Optional.of(AbsencePeriod.valueOf(raw));
                            } catch (IllegalArgumentException ex) {
                                return Optional.empty();
                            }
                        }),
                ListFilters.parseEnumList(
                        reason,
                        "reason",
                        raw -> {
                            try {
                                return Optional.of(AbsenceReason.valueOf(raw));
                            } catch (IllegalArgumentException ex) {
                                return Optional.empty();
                            }
                        }),
                ListFilters.optionalLongIds(organizationId),
                year == null ? List.of() : List.copyOf(year));
    }

    /**
     * Organization-wide absence list ({@code GET …/user-absences/active}): same filter params as
     * self lists; default period {@link AbsencePeriod#CURRENT} when {@code period} is absent.
     */
    public static AbsenceListFilter parseOrganizationActiveList(
            List<String> period, List<String> reason, List<Integer> year) {
        AbsenceListFilter parsed = parse(period, reason, List.of(), year);
        if (parsed.periods().isEmpty()) {
            return new AbsenceListFilter(
                    List.of(AbsencePeriod.CURRENT), parsed.reasons(), List.of(), parsed.years());
        }
        return parsed;
    }

    /** Sort override when exactly one period bucket is selected; otherwise default list order. */
    public String resolveSort() {
        if (periods.size() == 1) {
            return periods.get(0).sort();
        }
        return DEFAULT_SORT;
    }
}
