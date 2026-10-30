/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.event.domain.EventType;
import com.emberone.event.domain.ServiceEvent;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.platform.web.list.ListQuery;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class EventListSpecsTest {

    @BeforeEach
    void setUp() {
        OrganizationWallClockTime.configure(ZoneId.of("Europe/Berlin"));
    }

    @Test
    void emptyYearFilterIsUnrestricted() {
        EventListFilter filter = EventListFilter.parseOrg(null, null);
        assertThat(filter.years()).isEmpty();
        Specification<ServiceEvent> spec = EventListSpecs.overlapsCalendarYears(filter.years());
        assertThat(spec).isNotNull();
    }

    @Test
    void attendanceFilterParses() {
        EventListFilter filter =
                EventListFilter.parseOrg(null, List.of(EventListAttendanceStatus.ATTENDING.name()));
        assertThat(filter.attendance()).containsExactly(EventListAttendanceStatus.ATTENDING);
        Specification<ServiceEvent> spec =
                EventListSpecs.attendanceFilter(EventType.SERVICE, 1L, filter.attendance());
        assertThat(spec).isNotNull();
    }

    @Test
    void dateRangeFilterDetected() {
        Instant now = Instant.parse("2026-06-15T12:00:00Z");
        EventListFilter filter = EventListFilter.forDateRange(now, null);
        assertThat(filter.hasDateRange()).isTrue();
    }

    @Test
    void clientSortDescReversesEventLists() {
        assertThat(
                        OperationEventListDefinition.INSTANCE
                                .pageable(new ListQuery(0, null, null, "startAt,asc"))
                                .getSort()
                                .getOrderFor("start")
                                .getDirection())
                .isEqualTo(Sort.Direction.ASC);
        assertThat(
                        OperationEventListDefinition.INSTANCE
                                .pageable(new ListQuery(0, null, null, "startAt,desc"))
                                .getSort()
                                .getOrderFor("start")
                                .getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(
                        ServiceEventListDefinition.INSTANCE
                                .pageable(new ListQuery(0, null, null, "startAt,desc"))
                                .getSort()
                                .getOrderFor("start")
                                .getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(
                        ServiceEventListDefinition.INSTANCE
                                .pageable(new ListQuery(0, null, null, null))
                                .getSort()
                                .getOrderFor("start")
                                .getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }
}
