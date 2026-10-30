/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.list;

import com.emberone.event.domain.OperationEvent;
import com.emberone.platform.web.list.ListDefinition;

public final class OperationEventListDefinition {

    public static final ListDefinition<OperationEvent> INSTANCE =
            ListDefinition.<OperationEvent>forEntity(OperationEvent.class)
                    .searchField("number")
                    .searchField("keyword")
                    .searchField("tag")
                    .sortKey("startAt", "start")
                    .defaultSort("startAt,desc")
                    .build();

    private OperationEventListDefinition() {}
}
