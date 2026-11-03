/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.timeentry.list;

import com.emberone.platform.web.list.ListDefinition;
import com.emberone.timeentry.domain.UserTimeEntry;

public final class TimeEntryListDefinition {

    public static final ListDefinition<UserTimeEntry> INSTANCE =
            ListDefinition.<UserTimeEntry>forEntity(UserTimeEntry.class)
                    .searchField("user.firstName")
                    .searchField("user.lastName")
                    .sortKey("startAt", "startTime")
                    .textSortKey("lastName", "user.lastName", "user.firstName")
                    .defaultSort("startAt,desc")
                    .build();

    private TimeEntryListDefinition() {}
}
