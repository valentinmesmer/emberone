/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.list;

import com.emberone.event.domain.EventAttendance;
import com.emberone.event.domain.EventType;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.platform.web.list.ListSpecs;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class EventListSpecs {

    private EventListSpecs() {}

    public static <T> Specification<T> forOrganization(Long organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organization").get("id"), organizationId);
    }

    /** Filters on event {@code start}; {@code from} inclusive, {@code to} exclusive. */
    public static <T> Specification<T> startAtRange(Instant from, Instant to) {
        return (root, query, cb) ->
                ListSpecs.<T>range(root.get("start"), from, to).toPredicate(root, query, cb);
    }

    /** Overlap with {@code [fromInclusive, toExclusive)} on the event time span. */
    public static <T> Specification<T> overlapsRange(Instant fromInclusive, Instant toExclusive) {
        return (root, query, cb) ->
                cb.and(
                        cb.lessThan(root.get("start"), toExclusive),
                        cb.greaterThan(root.get("end"), fromInclusive));
    }

    public static <T> Specification<T> overlapsCalendarYears(List<Integer> years) {
        if (years == null || years.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        List<Specification<T>> buckets = new ArrayList<>();
        for (Integer year : years) {
            Instant from = yearStartInstant(year);
            Instant to = yearStartInstant(year + 1);
            buckets.add(overlapsRange(from, to));
        }
        return Specification.anyOf(buckets);
    }

    public static <T> Specification<T> attendanceFilter(
            EventType eventType, Long userId, List<EventListAttendanceStatus> attendance) {
        if (attendance == null || attendance.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        List<Specification<T>> buckets = new ArrayList<>();
        for (EventListAttendanceStatus status : attendance) {
            buckets.add(
                    switch (status) {
                        case ATTENDING -> attendedByUser(eventType, userId);
                        case NOT_ATTENDING -> notAttendedByUser(eventType, userId);
                    });
        }
        return Specification.anyOf(buckets);
    }

    private static Instant yearStartInstant(int year) {
        ZoneId zone = OrganizationWallClockTime.organizationZone();
        return LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant();
    }

    private static <T> Specification<T> attendedByUser(EventType eventType, Long userId) {
        return (root, query, cb) -> {
            Subquery<Long> match = query.subquery(Long.class);
            Root<EventAttendance> attendanceRoot = match.from(EventAttendance.class);
            match.select(cb.literal(1L));
            match.where(
                    cb.equal(attendanceRoot.get("eventType"), eventType),
                    cb.equal(attendanceRoot.get("eventId"), root.get("id")),
                    cb.equal(attendanceRoot.get("user").get("id"), userId));
            return cb.exists(match);
        };
    }

    private static <T> Specification<T> notAttendedByUser(EventType eventType, Long userId) {
        Specification<T> attended = attendedByUser(eventType, userId);
        return (root, query, cb) -> cb.not(attended.toPredicate(root, query, cb));
    }
}
