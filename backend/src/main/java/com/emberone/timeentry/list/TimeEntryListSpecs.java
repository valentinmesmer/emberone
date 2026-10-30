/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.list;

import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.platform.web.list.ListSpecs;
import com.emberone.timeentry.domain.UserTimeEntry;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class TimeEntryListSpecs {

    private TimeEntryListSpecs() {}

    public static Specification<UserTimeEntry> forUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<UserTimeEntry> forOrganization(Long organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organization").get("id"), organizationId);
    }

    public static Specification<UserTimeEntry> organizationsFilter(List<Long> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return ListSpecs.unconstrained();
        }
        return (root, query, cb) -> root.get("organization").get("id").in(organizationIds);
    }

    public static Specification<UserTimeEntry> startInCalendarYears(List<Integer> years) {
        if (years == null || years.isEmpty()) {
            return ListSpecs.unconstrained();
        }
        List<Specification<UserTimeEntry>> buckets = new ArrayList<>();
        for (Integer year : years) {
            buckets.add(startAtRange(yearStartInstant(year), yearStartInstant(year + 1)));
        }
        return Specification.anyOf(buckets);
    }

    private static Instant yearStartInstant(int year) {
        ZoneId zone = OrganizationWallClockTime.organizationZone();
        return LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant();
    }

    /** Filters on {@code startTime}; {@code from} inclusive, {@code to} exclusive. */
    public static Specification<UserTimeEntry> startAtRange(Instant from, Instant to) {
        if (from == null && to == null) {
            return ListSpecs.unconstrained();
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("startTime"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
