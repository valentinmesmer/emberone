/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.list;

import com.emberone.management.domain.ManagementActivation;
import com.emberone.platform.web.list.ListDefinition;

public final class ManagementActivationListDefinition {

    public static final ListDefinition<ManagementActivation> INSTANCE =
            ListDefinition.<ManagementActivation>forEntity(ManagementActivation.class)
                    .sortKey("activatedAt", "activatedAt")
                    .sortKey("validUntil", "validUntil")
                    .textSortKey("person", "user.lastName", "user.firstName")
                    .textSortKey("managingOrganization", "managingOrganization.name")
                    .textSortKey("targetOrganization", "targetOrganization.name")
                    .defaultSort("activatedAt,desc")
                    .build();

    private ManagementActivationListDefinition() {}
}
