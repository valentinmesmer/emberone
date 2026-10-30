/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.domain;

import com.emberone.userorganizationmembership.exception.MembershipStartDateInFutureException;
import java.time.Instant;

/** Active-range and overlap checks (half-open intervals) for organization and group memberships. */
public final class MembershipTimestampSupport {

    private MembershipTimestampSupport() {}

    public static void requireStartNotInTheFuture(Instant startDate) {
        if (startDate != null && startDate.isAfter(Instant.now())) {
            throw new MembershipStartDateInFutureException();
        }
    }

    public static void requireEndAfterStart(Instant startDate, Instant endDate) {
        if (endDate != null && !endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }

    /** Memberships are half-open intervals {@code [start, end)}: the end instant is not active. */
    public static boolean isActiveAt(Instant startAt, Instant endAt, Instant at) {
        if (startAt == null || at == null) {
            return false;
        }
        return !startAt.isAfter(at) && (endAt == null || endAt.isAfter(at));
    }

    /**
     * Whether {@code [start1, end1)} overlaps {@code [start2, end2)}; null end means open-ended.
     */
    public static boolean rangesOverlap(
            Instant start1, Instant end1, Instant start2, Instant end2) {
        boolean start1BeforeEnd2 = end2 == null || start1.isBefore(end2);
        boolean start2BeforeEnd1 = end1 == null || start2.isBefore(end1);
        return start1BeforeEnd2 && start2BeforeEnd1;
    }
}
