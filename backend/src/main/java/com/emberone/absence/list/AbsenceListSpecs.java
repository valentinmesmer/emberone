/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.list;

import com.emberone.absence.domain.AbsenceReason;
import com.emberone.absence.domain.UserAbsence;
import com.emberone.absence.domain.UserAbsenceOrganization;
import com.emberone.platform.time.OrganizationWallClockTime;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class AbsenceListSpecs {

    private AbsenceListSpecs() {}

    public static Specification<UserAbsence> forUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<UserAbsence> linkedToOrganization(Long organizationId) {
        return (root, query, cb) -> {
            Subquery<Long> linkMatch = query.subquery(Long.class);
            Root<UserAbsenceOrganization> linkRoot = linkMatch.from(UserAbsenceOrganization.class);
            linkMatch.select(cb.literal(1L));
            linkMatch.where(
                    cb.equal(linkRoot.get("absence"), root),
                    cb.equal(linkRoot.get("organization").get("id"), organizationId));
            return cb.exists(linkMatch);
        };
    }

    /** Absences that overlap {@code today} (inclusive date range). */
    public static Specification<UserAbsence> activeToday() {
        LocalDate today = OrganizationWallClockTime.today();
        return (root, query, cb) ->
                cb.and(
                        cb.lessThanOrEqualTo(root.get("startDate"), today),
                        cb.greaterThanOrEqualTo(root.get("endDate"), today));
    }

    /** Not ended before today (current and future absences). */
    public static Specification<UserAbsence> upcomingOrCurrent() {
        LocalDate today = OrganizationWallClockTime.today();
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("endDate"), today);
    }

    /** Ended before today. */
    public static Specification<UserAbsence> past() {
        LocalDate today = OrganizationWallClockTime.today();
        return (root, query, cb) -> cb.lessThan(root.get("endDate"), today);
    }

    public static Specification<UserAbsence> forPeriod(AbsencePeriod period) {
        return switch (period) {
            case CURRENT -> activeToday();
            case UPCOMING -> upcomingOrCurrent();
            case PAST -> past();
        };
    }

    public static Specification<UserAbsence> periodsFilter(List<AbsencePeriod> periods) {
        if (periods == null || periods.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        List<Specification<UserAbsence>> buckets = new ArrayList<>();
        for (AbsencePeriod period : periods) {
            buckets.add(forPeriod(period));
        }
        return Specification.anyOf(buckets);
    }

    public static Specification<UserAbsence> reasonsFilter(List<AbsenceReason> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> root.get("reason").in(reasons);
    }

    public static Specification<UserAbsence> linkedToAnyOrganization(List<Long> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            Subquery<Long> linkMatch = query.subquery(Long.class);
            Root<UserAbsenceOrganization> linkRoot = linkMatch.from(UserAbsenceOrganization.class);
            linkMatch.select(cb.literal(1L));
            linkMatch.where(
                    cb.equal(linkRoot.get("absence"), root),
                    linkRoot.get("organization").get("id").in(organizationIds));
            return cb.exists(linkMatch);
        };
    }

    /** Absence date range overlaps the calendar year {@code year} (inclusive). */
    public static Specification<UserAbsence> overlappingYear(Integer year) {
        if (year == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        return (root, query, cb) ->
                cb.and(
                        cb.lessThanOrEqualTo(root.get("startDate"), yearEnd),
                        cb.greaterThanOrEqualTo(root.get("endDate"), yearStart));
    }

    public static Specification<UserAbsence> overlappingYears(List<Integer> years) {
        if (years == null || years.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        List<Specification<UserAbsence>> buckets = new ArrayList<>();
        for (Integer year : years) {
            buckets.add(overlappingYear(year));
        }
        return Specification.anyOf(buckets);
    }
}
