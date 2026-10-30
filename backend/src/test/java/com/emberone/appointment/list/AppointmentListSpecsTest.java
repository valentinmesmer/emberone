/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.appointment.domain.Appointment;
import com.emberone.platform.web.list.ListQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class AppointmentListSpecsTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

    @SuppressWarnings("unchecked")
    @Test
    void upcomingIncludesAppointmentsEndingExactlyNow() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<Appointment> root = mock(Root.class);
        when(root.get("end")).thenReturn(mock(Path.class));

        AppointmentListSpecs.forPeriod(AppointmentPeriod.UPCOMING, NOW)
                .toPredicate(root, mock(CriteriaQuery.class), cb);

        verify(cb).greaterThanOrEqualTo(any(Path.class), eq(NOW));
    }

    @SuppressWarnings("unchecked")
    @Test
    void pastExcludesAppointmentsEndingExactlyNow() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<Appointment> root = mock(Root.class);
        when(root.get("end")).thenReturn(mock(Path.class));

        AppointmentListSpecs.forPeriod(AppointmentPeriod.PAST, NOW)
                .toPredicate(root, mock(CriteriaQuery.class), cb);

        verify(cb).lessThan(any(Path.class), eq(NOW));
    }

    @Test
    void emptyPeriodUsesUpcomingSortByDefault() {
        AppointmentListFilter filter = AppointmentListFilter.parseOrg(null, null, null, null);
        assertThat(sortOf(filter.sortOverride()))
                .startsWith(new Sort.Order(Sort.Direction.ASC, "start"));
    }

    @Test
    void pastSortsDescending() {
        AppointmentListFilter filter =
                AppointmentListFilter.parseOrg(List.of("PAST"), null, null, null);
        assertThat(sortOf(filter.sortOverride()))
                .startsWith(new Sort.Order(Sort.Direction.DESC, "start"));
    }

    @Test
    void explicitWindowIgnoresPeriodSortOverride() {
        AppointmentListFilter filter = AppointmentListFilter.forDateRange(NOW, null);
        assertThat(filter.hasDateRange()).isTrue();
        assertThat(filter.sortOverride()).isNull();
    }

    private static java.util.List<Sort.Order> sortOf(String override) {
        return AppointmentListDefinition.INSTANCE
                .pageable(new ListQuery(0, null, null, null), override)
                .getSort()
                .toList();
    }

    @Test
    void clientSortDescReversesAndBeatsPeriodOverride() {
        AppointmentListFilter upcoming =
                AppointmentListFilter.parseOrg(List.of("UPCOMING"), null, null, null);
        var orders =
                AppointmentListDefinition.INSTANCE
                        .pageable(
                                new ListQuery(0, null, null, "start,desc"), upcoming.sortOverride())
                        .getSort()
                        .toList();
        assertThat(orders).startsWith(new Sort.Order(Sort.Direction.DESC, "start"));

        var textOrders =
                AppointmentListDefinition.INSTANCE
                        .pageable(
                                new ListQuery(0, null, null, "name,desc"), upcoming.sortOverride())
                        .getSort()
                        .toList();
        assertThat(textOrders).startsWith(new Sort.Order(Sort.Direction.DESC, "name").ignoreCase());
    }

    @Test
    void explicitWindowWithoutClientSortIsStartAscending() {
        AppointmentListFilter window = AppointmentListFilter.forDateRange(NOW, null);
        assertThat(sortOf(window.sortOverride()))
                .startsWith(new Sort.Order(Sort.Direction.ASC, "start"));
    }
}
