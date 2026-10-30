/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.list;

import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.platform.web.list.ListDefinition;

public final class TenantListDefinition {

    public static final ListDefinition<Tenant> INSTANCE =
            ListDefinition.<Tenant>forEntity(Tenant.class)
                    .searchField("name")
                    .searchField("abbreviation")
                    .textSortKey("name", "name")
                    .defaultSort("name,asc")
                    .build();

    private TenantListDefinition() {}
}
