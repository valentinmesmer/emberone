/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.list;

import com.emberone.event.domain.EventAttendance;
import com.emberone.platform.web.list.ListDefinition;

public final class EventAttendanceListDefinition {

    public static final ListDefinition<EventAttendance> INSTANCE =
            ListDefinition.<EventAttendance>forEntity(EventAttendance.class)
                    .searchField("user.firstName")
                    .searchField("user.lastName")
                    .textSortKey("lastName", "user.lastName", "user.firstName")
                    .defaultSort("lastName,asc")
                    .build();

    private EventAttendanceListDefinition() {}
}
