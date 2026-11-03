/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.event.list;

import com.emberone.event.domain.ServiceEvent;
import com.emberone.platform.web.list.ListDefinition;

public final class ServiceEventListDefinition {

    public static final ListDefinition<ServiceEvent> INSTANCE =
            ListDefinition.<ServiceEvent>forEntity(ServiceEvent.class)
                    .searchField("topic")
                    .sortKey("startAt", "start")
                    .defaultSort("startAt,desc")
                    .build();

    private ServiceEventListDefinition() {}
}
