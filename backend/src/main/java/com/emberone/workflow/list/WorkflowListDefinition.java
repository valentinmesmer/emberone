/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.list;

import com.emberone.platform.web.list.ListDefinition;
import com.emberone.workflow.domain.Workflow;

public final class WorkflowListDefinition {

    public static final ListDefinition<Workflow> INSTANCE =
            ListDefinition.<Workflow>forEntity(Workflow.class)
                    .sortKey("openedAt", "openedAt")
                    .defaultSort("openedAt,desc")
                    .build();

    private WorkflowListDefinition() {}
}
