/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
