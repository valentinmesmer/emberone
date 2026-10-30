/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class MembershipTimestampSupportTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant BOUNDARY = Instant.parse("2026-07-01T00:00:00Z");

    @Test
    void intervalIsHalfOpen() {
        assertThat(MembershipTimestampSupport.isActiveAt(START, BOUNDARY, START)).isTrue();
        assertThat(MembershipTimestampSupport.isActiveAt(START, BOUNDARY, BOUNDARY)).isFalse();
        assertThat(MembershipTimestampSupport.isActiveAt(START, null, BOUNDARY)).isTrue();
    }

    @Test
    void adjacentRangesDoNotOverlap() {
        assertThat(MembershipTimestampSupport.rangesOverlap(START, BOUNDARY, BOUNDARY, null))
                .isFalse();
        assertThat(MembershipTimestampSupport.rangesOverlap(BOUNDARY, null, START, BOUNDARY))
                .isFalse();
    }

    @Test
    void overlappingRangesAreDetected() {
        assertThat(
                        MembershipTimestampSupport.rangesOverlap(
                                START, BOUNDARY.plusMillis(1), BOUNDARY, null))
                .isTrue();
        assertThat(MembershipTimestampSupport.rangesOverlap(START, null, BOUNDARY, null)).isTrue();
    }

    @Test
    void endMustBeAfterStart() {
        assertThatThrownBy(() -> MembershipTimestampSupport.requireEndAfterStart(START, START))
                .isInstanceOf(IllegalArgumentException.class);
        MembershipTimestampSupport.requireEndAfterStart(START, BOUNDARY);
        MembershipTimestampSupport.requireEndAfterStart(START, null);
    }
}
