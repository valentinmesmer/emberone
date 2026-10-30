/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
