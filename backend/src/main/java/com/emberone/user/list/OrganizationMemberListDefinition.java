/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.list;

import com.emberone.platform.web.list.ListDefinition;
import com.emberone.user.domain.User;

public final class OrganizationMemberListDefinition {

    public static final ListDefinition<User> INSTANCE =
            ListDefinition.<User>forEntity(User.class)
                    .searchField("firstName")
                    .searchField("lastName")
                    .textSortKey("lastName", "lastName", "firstName")
                    .textSortKey("firstName", "firstName", "lastName")
                    .defaultSort("lastName,asc")
                    .build();

    private OrganizationMemberListDefinition() {}
}
