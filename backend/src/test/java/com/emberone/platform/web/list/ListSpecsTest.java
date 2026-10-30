/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ListSpecsTest {

    @Test
    void searchTokensSplitsAndLowercases() {
        assertThat(ListSpecs.searchTokens("  Foo  BAR ")).containsExactly("foo", "bar");
    }

    @Test
    void searchTokensRejectsTooManyWords() {
        assertThatThrownBy(() -> ListSpecs.searchTokens("one two three four five six"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void escapeLikeEscapesWildcards() {
        assertThat(ListSpecs.escapeLike("100%_done\\")).isEqualTo("100\\%\\_done\\\\");
    }
}
