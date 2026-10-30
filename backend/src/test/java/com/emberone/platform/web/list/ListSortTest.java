/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class ListSortTest {

    @Test
    void parsesKeyAndDirection() {
        assertThat(ListSort.parse(" name , DESC "))
                .isEqualTo(new ListSort("name", Sort.Direction.DESC));
        assertThat(ListSort.parse("a.b,asc").key()).isEqualTo("a.b");
    }

    @Test
    void rejectsBadFormatAndDirection() {
        assertThatThrownBy(() -> ListSort.parse(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ListSort.parse("name"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ListSort.parse(",asc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ListSort.parse("name,up"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
