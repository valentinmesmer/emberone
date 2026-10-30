/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.list;

import com.emberone.appointment.domain.Appointment;
import com.emberone.platform.web.list.ListDefinition;

public final class AppointmentListDefinition {

    public static final ListDefinition<Appointment> INSTANCE =
            ListDefinition.<Appointment>forEntity(Appointment.class)
                    .searchField("name")
                    .sortKey("start", "start")
                    .textSortKey("name", "name")
                    .defaultSort("start,asc")
                    .build();

    private AppointmentListDefinition() {}
}
