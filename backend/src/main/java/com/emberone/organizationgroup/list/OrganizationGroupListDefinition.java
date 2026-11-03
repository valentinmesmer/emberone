/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.organizationgroup.list;

import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.platform.web.list.ListDefinition;

public final class OrganizationGroupListDefinition {

    public static final ListDefinition<OrganizationGroup> INSTANCE =
            ListDefinition.<OrganizationGroup>forEntity(OrganizationGroup.class)
                    .searchField("name")
                    .sortKey("name", "name")
                    .defaultSort("name,asc")
                    .build();

    private OrganizationGroupListDefinition() {}
}
