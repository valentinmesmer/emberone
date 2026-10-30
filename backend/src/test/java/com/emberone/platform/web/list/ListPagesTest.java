/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

class ListPagesTest {

    @Test
    void sliceReturnsPageWindow() {
        Page<String> page =
                ListPages.slice(List.of("a", "b", "c", "d"), new ListQuery(1, 2, null, null));

        assertThat(page.getContent()).containsExactly("c", "d");
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.hasNext()).isFalse();
    }
}
