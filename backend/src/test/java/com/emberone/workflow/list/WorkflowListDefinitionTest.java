/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.platform.web.list.ListQuery;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class WorkflowListDefinitionTest {

    @Test
    void defaultSortUsesOpenedAtDescWithIdTiebreaker() {
        var pageable = WorkflowListDefinition.INSTANCE.pageable(new ListQuery(0, 50, null, null));
        Sort sort = pageable.getSort();
        assertThat(sort.getOrderFor("openedAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sort.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void sortDescReversesOpenedAtAndRejectsRemovedKey() {
        Sort asc =
                WorkflowListDefinition.INSTANCE
                        .pageable(new ListQuery(0, 50, null, "openedAt,asc"))
                        .getSort();
        Sort desc =
                WorkflowListDefinition.INSTANCE
                        .pageable(new ListQuery(0, 50, null, "openedAt,desc"))
                        .getSort();
        assertThat(asc.getOrderFor("openedAt").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(desc.getOrderFor("openedAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void removedSortKeyIsRejected() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () ->
                                WorkflowListDefinition.INSTANCE.pageable(
                                        new ListQuery(0, 50, null, "decidedAt,asc")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
