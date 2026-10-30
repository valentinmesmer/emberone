/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.user.list;

import com.emberone.identity.domain.IdentityUser;
import com.emberone.platform.web.list.ListDefinition;

public final class AdminUserListDefinition {

    public static final ListDefinition<IdentityUser> INSTANCE =
            ListDefinition.<IdentityUser>forEntity(IdentityUser.class)
                    .searchField("givenName")
                    .searchField("surname")
                    .searchField("username")
                    .textSortKey("lastName", "surname", "givenName")
                    .textSortKey("firstName", "givenName", "surname")
                    .defaultSort("lastName,asc")
                    .build();

    private AdminUserListDefinition() {}
}
