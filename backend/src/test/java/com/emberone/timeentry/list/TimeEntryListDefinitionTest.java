/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.emberone.platform.web.list.ListQuery;
import com.emberone.timeentry.domain.UserTimeEntry;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Sort;

class TimeEntryListDefinitionTest {

    @SuppressWarnings("unchecked")
    @Test
    void searchMatchesUserNameNotDescription() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class, Mockito.RETURNS_DEEP_STUBS);
        Root<UserTimeEntry> root = mock(Root.class, Mockito.RETURNS_DEEP_STUBS);

        TimeEntryListDefinition.INSTANCE
                .searchSpecification(new ListQuery(0, null, "Max Muster", null))
                .toPredicate(root, mock(CriteriaQuery.class), cb);

        verify(root, atLeastOnce()).get("user");
        verify(root, never()).get("description");
    }

    @Test
    void sortDescReversesAndDefaultIsStartDesc() {
        var desc =
                TimeEntryListDefinition.INSTANCE
                        .pageable(new ListQuery(0, null, null, "startAt,asc"))
                        .getSort();
        assertThat(desc.getOrderFor("startTime").getDirection()).isEqualTo(Sort.Direction.ASC);
        var rev =
                TimeEntryListDefinition.INSTANCE
                        .pageable(new ListQuery(0, null, null, "startAt,desc"))
                        .getSort();
        assertThat(rev.getOrderFor("startTime").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(
                        TimeEntryListDefinition.INSTANCE
                                .pageable(new ListQuery(0, null, null, "lastName,desc"))
                                .getSort()
                                .getOrderFor("user.firstName")
                                .getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(
                        TimeEntryListDefinition.INSTANCE
                                .pageable(new ListQuery(0, null, null, null))
                                .getSort()
                                .getOrderFor("startTime")
                                .getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }
}
