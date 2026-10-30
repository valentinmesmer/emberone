/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.platform.web.list.ListQuery;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class OrganizationRankMemberListDefinitionTest {

    @Test
    void lastNameSortUsesDefinitionPageable() {
        Pageable pageable =
                OrganizationRankMemberListDefinition.INSTANCE.pageable(
                        new ListQuery(0, 50, null, "lastName,asc"));
        assertThat(pageable.getSort().getOrderFor("user.lastName")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("user.firstName")).isNotNull();
    }
}
