/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.appointment.list;

/** Time window filter of the appointment list, relative to the current instant. */
public enum AppointmentPeriod {
    /** {@code end >= now}, sorted by start ascending. */
    UPCOMING("start,asc"),
    /** {@code end < now}, sorted by start descending. */
    PAST("start,desc");

    private final String sort;

    AppointmentPeriod(String sort) {
        this.sort = sort;
    }

    /** Sort expression passed as {@code sortOverride} to the list definition. */
    public String sort() {
        return sort;
    }
}
