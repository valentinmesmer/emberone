/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.list;

import com.emberone.absence.domain.UserAbsence;
import com.emberone.platform.web.list.ListDefinition;

public final class AbsenceListDefinition {

    public static final ListDefinition<UserAbsence> INSTANCE =
            ListDefinition.<UserAbsence>forEntity(UserAbsence.class)
                    .searchField("user.firstName")
                    .searchField("user.lastName")
                    .sortKey("startDate", "startDate")
                    .textSortKey("lastName", "user.lastName", "user.firstName")
                    .defaultSort("startDate,desc")
                    .build();

    private AbsenceListDefinition() {}
}
