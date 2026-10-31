/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.absence.list;

/** Time window filter of absence lists, relative to today in organization wall-clock time. */
public enum AbsencePeriod {
    /** {@code startDate <= today <= endDate}, sorted by start ascending. */
    CURRENT("startDate,asc"),
    /** {@code endDate >= today} (current and future), sorted by start ascending. */
    UPCOMING("startDate,asc"),
    /** {@code endDate < today}, sorted by start descending. */
    PAST("startDate,desc");

    private final String sort;

    AbsencePeriod(String sort) {
        this.sort = sort;
    }

    /** Sort expression passed as {@code sortOverride} to the list definition. */
    public String sort() {
        return sort;
    }
}
