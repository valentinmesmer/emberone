/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.absence.domain.UserAbsence;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.platform.web.list.ListQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class AbsenceListSpecsTest {

    @SuppressWarnings("unchecked")
    private static void apply(
            org.springframework.data.jpa.domain.Specification<UserAbsence> spec,
            CriteriaBuilder cb,
            Root<UserAbsence> root) {
        spec.toPredicate(root, mock(CriteriaQuery.class), cb);
    }

    @SuppressWarnings("unchecked")
    @Test
    void upcomingIncludesAbsencesEndingToday() {
        LocalDate today = OrganizationWallClockTime.today();
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<UserAbsence> root = mock(Root.class);
        Path<Object> endDate = mock(Path.class);
        when(root.get("endDate")).thenReturn(endDate);

        apply(AbsenceListSpecs.forPeriod(AbsencePeriod.UPCOMING), cb, root);

        verify(cb).greaterThanOrEqualTo(any(Path.class), org.mockito.ArgumentMatchers.eq(today));
    }

    @SuppressWarnings("unchecked")
    @Test
    void pastExcludesAbsencesEndingToday() {
        LocalDate today = OrganizationWallClockTime.today();
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<UserAbsence> root = mock(Root.class);
        when(root.get("endDate")).thenReturn(mock(Path.class));

        apply(AbsenceListSpecs.forPeriod(AbsencePeriod.PAST), cb, root);

        verify(cb).lessThan(any(Path.class), org.mockito.ArgumentMatchers.eq(today));
    }

    @SuppressWarnings("unchecked")
    @Test
    void currentIsInclusiveOnBothBounds() {
        LocalDate today = OrganizationWallClockTime.today();
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<UserAbsence> root = mock(Root.class);
        when(root.get("startDate")).thenReturn(mock(Path.class));
        when(root.get("endDate")).thenReturn(mock(Path.class));

        apply(AbsenceListSpecs.forPeriod(AbsencePeriod.CURRENT), cb, root);

        verify(cb).lessThanOrEqualTo(any(Path.class), org.mockito.ArgumentMatchers.eq(today));
        verify(cb).greaterThanOrEqualTo(any(Path.class), org.mockito.ArgumentMatchers.eq(today));
    }

    @Test
    void emptyPeriodFilterUsesDefaultSort() {
        assertThat(AbsenceListFilter.empty().resolveSort()).isEqualTo(AbsencePeriod.PAST.sort());
    }

    @Test
    void sortDependsOnPeriod() {
        ListQuery query = new ListQuery(0, null, null, null);
        assertThat(
                        sortOf(
                                query,
                                AbsenceListFilter.parseOrganizationActiveList(
                                        List.of(AbsencePeriod.CURRENT.name()),
                                        List.of(),
                                        List.of())))
                .startsWith(new Sort.Order(Sort.Direction.ASC, "startDate"));
        assertThat(
                        sortOf(
                                query,
                                AbsenceListFilter.parseOrganizationActiveList(
                                        List.of(AbsencePeriod.UPCOMING.name()),
                                        List.of(),
                                        List.of())))
                .startsWith(new Sort.Order(Sort.Direction.ASC, "startDate"));
        assertThat(
                        sortOf(
                                query,
                                AbsenceListFilter.parseOrganizationActiveList(
                                        List.of(AbsencePeriod.PAST.name()), List.of(), List.of())))
                .startsWith(new Sort.Order(Sort.Direction.DESC, "startDate"));
    }

    @Test
    void clientSortOverridesPeriodSort() {
        ListQuery query = new ListQuery(0, null, null, "startDate,asc");
        assertThat(
                        sortOf(
                                query,
                                AbsenceListFilter.parseOrganizationActiveList(
                                        List.of(AbsencePeriod.PAST.name()), List.of(), List.of())))
                .startsWith(new Sort.Order(Sort.Direction.ASC, "startDate"));
    }

    private static java.util.List<Sort.Order> sortOf(ListQuery query, AbsenceListFilter filter) {
        return AbsenceListDefinition.INSTANCE
                .pageable(query, filter.resolveSort())
                .getSort()
                .toList();
    }

    @SuppressWarnings("unchecked")
    @Test
    void searchMatchesUserFirstAndLastNameNotComment() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        Root<UserAbsence> root = mock(Root.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);

        AbsenceListDefinition.INSTANCE
                .searchSpecification(new ListQuery(0, null, "Max Muster", null))
                .toPredicate(root, mock(CriteriaQuery.class), cb);

        verify(root, org.mockito.Mockito.atLeastOnce()).get("user");
        verify(root, org.mockito.Mockito.never()).get("comment");
    }

    @Test
    void clientSortDescReversesAndMultiColumnNameSort() {
        AbsenceListFilter past =
                AbsenceListFilter.parseOrganizationActiveList(
                        List.of(AbsencePeriod.PAST.name()), List.of(), List.of());
        assertThat(sortOf(new ListQuery(0, null, null, "startDate,asc"), past))
                .startsWith(new Sort.Order(Sort.Direction.ASC, "startDate"));
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> sortOf(new ListQuery(0, null, null, "endDate,desc"), past))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(sortOf(new ListQuery(0, null, null, "lastName,desc"), past))
                .startsWith(
                        new Sort.Order(Sort.Direction.DESC, "user.lastName").ignoreCase(),
                        new Sort.Order(Sort.Direction.DESC, "user.firstName").ignoreCase());
        assertThat(sortOf(new ListQuery(0, null, null, null), past))
                .startsWith(new Sort.Order(Sort.Direction.DESC, "startDate"));
    }
}
