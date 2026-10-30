/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.platform.security.EmberOneAppRole;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListFiltersTest {

    @Test
    void optionalLongIds_nullOrEmpty() {
        assertThat(ListFilters.optionalLongIds(null)).isEmpty();
        assertThat(ListFilters.optionalLongIds(List.of())).isEmpty();
    }

    @Test
    void parseEnumList_skipsBlankAndTrims() {
        assertThat(
                        ListFilters.parseEnumList(
                                List.of(" tenant_admin ", " ", "GLOBAL_ADMIN"),
                                "appRole",
                                EmberOneAppRole::fromRoleName))
                .containsExactly(EmberOneAppRole.TENANT_ADMIN, EmberOneAppRole.GLOBAL_ADMIN);
    }

    @Test
    void parseEnumList_invalidThrows() {
        assertThatThrownBy(
                        () ->
                                ListFilters.parseEnumList(
                                        List.of("SUPERUSER"),
                                        "appRole",
                                        EmberOneAppRole::fromRoleName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("appRole");
    }
}
