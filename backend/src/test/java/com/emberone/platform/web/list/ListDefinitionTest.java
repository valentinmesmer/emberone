/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.todo.list.TodoListDefinition;
import com.emberone.todo.list.TodoListStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class ListDefinitionTest {

    @Test
    void pageableAppliesDefaultSortAndIdTiebreaker() {
        var pageable =
                TodoListDefinition.INSTANCE.pageable(
                        new ListQuery(0, 50, null, null), "createdAt,desc");
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(50);
        assertThat(pageable.getSort().toList())
                .containsExactly(
                        new Sort.Order(Sort.Direction.DESC, "createdAt"),
                        new Sort.Order(Sort.Direction.ASC, "id"));
    }

    @Test
    void pageableRejectsUnknownSortField() {
        assertThatThrownBy(
                        () ->
                                TodoListDefinition.INSTANCE.pageable(
                                        new ListQuery(0, null, null, "unknown,asc"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void todoStatusOverridesSort() {
        var query = new ListQuery(0, 50, null, null);
        assertThat(
                        TodoListDefinition.INSTANCE
                                .pageable(query, TodoListStatus.OPEN.sort())
                                .getSort()
                                .toList())
                .containsExactly(
                        new Sort.Order(Sort.Direction.DESC, "createdAt"),
                        new Sort.Order(Sort.Direction.ASC, "id"));
        assertThat(
                        TodoListDefinition.INSTANCE
                                .pageable(query, TodoListStatus.DONE.sort())
                                .getSort()
                                .toList())
                .containsExactly(
                        new Sort.Order(Sort.Direction.DESC, "archivedAt"),
                        new Sort.Order(Sort.Direction.ASC, "id"));
    }

    private static final ListDefinition<Object> DEFINITION =
            ListDefinition.forEntity(Object.class)
                    .searchField("name")
                    .sortKey("createdAt", "createdAt")
                    .sortKey("id", "id")
                    .textSortKey("name", "last", "first")
                    .defaultSort("createdAt,desc")
                    .build();

    @Test
    void clientSortBeatsOverrideBeatsDefault() {
        var none = new ListQuery(0, 50, null, null);
        var client = new ListQuery(0, 50, null, "createdAt,asc");
        assertThat(DEFINITION.pageable(none).getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(
                        DEFINITION
                                .pageable(none, "createdAt,asc")
                                .getSort()
                                .getOrderFor("createdAt")
                                .getDirection())
                .isEqualTo(Sort.Direction.ASC);
        assertThat(
                        DEFINITION
                                .pageable(client, "createdAt,desc")
                                .getSort()
                                .getOrderFor("createdAt")
                                .getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void multiPropertyTextKeyIsCaseInsensitiveWithIdTiebreaker() {
        var sort = DEFINITION.pageable(new ListQuery(0, 50, null, "name,desc")).getSort();
        assertThat(sort.toList())
                .containsExactly(
                        new Sort.Order(Sort.Direction.DESC, "last").ignoreCase(),
                        new Sort.Order(Sort.Direction.DESC, "first").ignoreCase(),
                        new Sort.Order(Sort.Direction.ASC, "id"));
    }

    @Test
    void idKeyHasNoSecondIdOrder() {
        var sort = DEFINITION.pageable(new ListQuery(0, 50, null, "id,desc")).getSort();
        assertThat(sort.toList()).containsExactly(new Sort.Order(Sort.Direction.DESC, "id"));
    }

    @Test
    void invalidKeyOrDirectionIsRejected() {
        assertThatThrownBy(() -> DEFINITION.pageable(new ListQuery(0, 50, null, "nope,asc")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DEFINITION.pageable(new ListQuery(0, 50, null, "name,up")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DEFINITION.pageable(new ListQuery(0, 50, null, "name")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
