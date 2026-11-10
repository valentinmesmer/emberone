/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.management.list;

import com.emberone.management.domain.ManagementActivation;
import com.emberone.platform.web.list.ListDefinition;

public final class ManagementActivationListDefinition {

    public static final ListDefinition<ManagementActivation> INSTANCE =
            ListDefinition.<ManagementActivation>forEntity(ManagementActivation.class)
                    .sortKey("activatedAt", "activatedAt")
                    .textSortKey("person", "user.lastName", "user.firstName")
                    .defaultSort("activatedAt,desc")
                    .build();

    private ManagementActivationListDefinition() {}
}
