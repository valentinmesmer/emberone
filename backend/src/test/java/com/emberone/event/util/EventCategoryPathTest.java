/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.event.domain.EventType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EventCategoryPathTest {

    @ParameterizedTest
    @CsvSource({
        "service, SERVICE",
        "operation, OPERATION",
    })
    void mapsKnownSegments(String segment, EventType expected) {
        assertThat(EventCategoryPath.toEventType(segment)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "SERVICE, service",
        "OPERATION, operation",
    })
    void mapsKnownEventTypes(EventType eventType, String expected) {
        assertThat(EventCategoryPath.fromEventType(eventType)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"training", "public-relations", "unknown"})
    void rejectsLegacyOrUnknownSegments(String segment) {
        assertThatThrownBy(() -> EventCategoryPath.toEventType(segment))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
