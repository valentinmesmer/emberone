/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationgroupmembership.list;

import com.emberone.platform.web.list.ListDefinition;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;

public final class OrganizationGroupMemberListDefinition {

    public static final ListDefinition<UserOrganizationGroupMembership> INSTANCE =
            ListDefinition.<UserOrganizationGroupMembership>forEntity(
                            UserOrganizationGroupMembership.class)
                    .searchField("user.firstName")
                    .searchField("user.lastName")
                    .textSortKey("lastName", "user.lastName", "user.firstName")
                    .textSortKey("firstName", "user.firstName", "user.lastName")
                    .defaultSort("lastName,asc")
                    .build();

    private OrganizationGroupMemberListDefinition() {}
}
