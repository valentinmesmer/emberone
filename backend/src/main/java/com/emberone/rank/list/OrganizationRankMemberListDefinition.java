/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.list;

import com.emberone.platform.web.list.ListDefinition;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;

public final class OrganizationRankMemberListDefinition {

    public static final ListDefinition<UserOrganizationMembership> INSTANCE =
            ListDefinition.<UserOrganizationMembership>forEntity(UserOrganizationMembership.class)
                    .searchField("user.firstName")
                    .searchField("user.lastName")
                    .textSortKey("lastName", "user.lastName", "user.firstName")
                    .textSortKey("firstName", "user.firstName", "user.lastName")
                    .defaultSort("lastName,asc")
                    .build();

    private OrganizationRankMemberListDefinition() {}
}
