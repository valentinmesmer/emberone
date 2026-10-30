/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.list;

import com.emberone.appointment.domain.Appointment;
import com.emberone.appointment.domain.AppointmentAudienceGroup;
import com.emberone.appointment.domain.AppointmentRead;
import com.emberone.appointment.domain.AppointmentResponse;
import com.emberone.appointment.domain.AppointmentResponseMode;
import com.emberone.appointment.domain.AppointmentResponseValue;
import com.emberone.appointment.domain.AudienceScope;
import com.emberone.platform.time.OrganizationWallClockTime;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class AppointmentListSpecs {

    private AppointmentListSpecs() {}

    public static Specification<Appointment> forOrganization(Long organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organization").get("id"), organizationId);
    }

    public static Specification<Appointment> activeOnly() {
        return (root, query, cb) -> cb.isFalse(root.get("archived"));
    }

    /** Overlap with {@code [fromInclusive, toExclusive)} on the appointment time span. */
    public static Specification<Appointment> overlapsRange(
            Instant fromInclusive, Instant toExclusive) {
        return (root, query, cb) ->
                cb.and(
                        cb.lessThan(root.get("start"), toExclusive),
                        cb.greaterThan(root.get("end"), fromInclusive));
    }

    /** Not yet ended at {@code now}. */
    public static Specification<Appointment> upcoming(Instant now) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("end"), now);
    }

    /** Ended before {@code now}. */
    public static Specification<Appointment> past(Instant now) {
        return (root, query, cb) -> cb.lessThan(root.get("end"), now);
    }

    public static Specification<Appointment> forPeriod(AppointmentPeriod period, Instant now) {
        return switch (period) {
            case UPCOMING -> upcoming(now);
            case PAST -> past(now);
        };
    }

    public static Specification<Appointment> periodsFilter(
            List<AppointmentPeriod> periods, Instant now) {
        if (periods == null || periods.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        List<Specification<Appointment>> buckets = new ArrayList<>();
        for (AppointmentPeriod period : periods) {
            buckets.add(forPeriod(period, now));
        }
        return Specification.anyOf(buckets);
    }

    public static Specification<Appointment> overlapsCalendarYears(List<Integer> years) {
        if (years == null || years.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        List<Specification<Appointment>> buckets = new ArrayList<>();
        for (Integer year : years) {
            Instant from = yearStartInstant(year);
            Instant to = yearStartInstant(year + 1);
            buckets.add(overlapsRange(from, to));
        }
        return Specification.anyOf(buckets);
    }

    public static Specification<Appointment> viewedFilter(
            Long userId, List<AppointmentListViewedStatus> viewed) {
        if (viewed == null || viewed.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        List<Specification<Appointment>> buckets = new ArrayList<>();
        for (AppointmentListViewedStatus status : viewed) {
            buckets.add(
                    switch (status) {
                        case READ -> hasReadByUser(userId);
                        case UNREAD -> notReadByUser(userId);
                    });
        }
        return Specification.anyOf(buckets);
    }

    public static Specification<Appointment> responseFilter(
            Long userId, List<AppointmentListResponseStatus> response) {
        if (response == null || response.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        List<Specification<Appointment>> buckets = new ArrayList<>();
        for (AppointmentListResponseStatus status : response) {
            buckets.add(
                    switch (status) {
                        case YES -> responseValueForUser(userId, AppointmentResponseValue.YES);
                        case NO -> responseValueForUser(userId, AppointmentResponseValue.NO);
                        case UNCERTAIN ->
                                responseValueForUser(userId, AppointmentResponseValue.UNCERTAIN);
                        case NO_RESPONSE -> pendingResponseFromUser(userId);
                    });
        }
        return Specification.anyOf(buckets);
    }

    private static Instant yearStartInstant(int year) {
        ZoneId zone = OrganizationWallClockTime.organizationZone();
        return LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant();
    }

    private static Specification<Appointment> hasReadByUser(Long userId) {
        return (root, query, cb) -> {
            Subquery<Long> match = query.subquery(Long.class);
            Root<AppointmentRead> readRoot = match.from(AppointmentRead.class);
            match.select(cb.literal(1L));
            match.where(
                    cb.equal(readRoot.get("appointment"), root),
                    cb.equal(readRoot.get("userId"), userId));
            return cb.exists(match);
        };
    }

    private static Specification<Appointment> notReadByUser(Long userId) {
        return (root, query, cb) -> cb.not(hasReadByUser(userId).toPredicate(root, query, cb));
    }

    private static Specification<Appointment> responseValueForUser(
            Long userId, AppointmentResponseValue value) {
        return (root, query, cb) -> {
            Subquery<Long> match = query.subquery(Long.class);
            Root<AppointmentResponse> responseRoot = match.from(AppointmentResponse.class);
            match.select(cb.literal(1L));
            match.where(
                    cb.equal(responseRoot.get("appointment"), root),
                    cb.equal(responseRoot.get("userId"), userId),
                    cb.equal(responseRoot.get("value"), value));
            return cb.exists(match);
        };
    }

    /** Response possible, but the user has not answered yet. */
    private static Specification<Appointment> pendingResponseFromUser(Long userId) {
        return (root, query, cb) ->
                cb.and(
                        cb.notEqual(root.get("responseMode"), AppointmentResponseMode.NONE),
                        cb.not(hasResponseFromUser(userId).toPredicate(root, query, cb)));
    }

    private static Specification<Appointment> hasResponseFromUser(Long userId) {
        return (root, query, cb) -> {
            Subquery<Long> match = query.subquery(Long.class);
            Root<AppointmentResponse> responseRoot = match.from(AppointmentResponse.class);
            match.select(cb.literal(1L));
            match.where(
                    cb.equal(responseRoot.get("appointment"), root),
                    cb.equal(responseRoot.get("userId"), userId));
            return cb.exists(match);
        };
    }

    public static Specification<Appointment> visibleToUser(List<Long> activeGroupIds) {
        return (root, query, cb) -> {
            var allAudience = cb.equal(root.get("audienceScope"), AudienceScope.ALL);
            if (activeGroupIds == null || activeGroupIds.isEmpty()) {
                return allAudience;
            }
            Subquery<Long> groupMatch = query.subquery(Long.class);
            Root<AppointmentAudienceGroup> groupRoot =
                    groupMatch.from(AppointmentAudienceGroup.class);
            groupMatch.select(cb.literal(1L));
            groupMatch.where(
                    cb.equal(groupRoot.get("appointment"), root),
                    groupRoot.get("id").get("organizationGroupId").in(activeGroupIds));
            return cb.or(allAudience, cb.exists(groupMatch));
        };
    }
}
