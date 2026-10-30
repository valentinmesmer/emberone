/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.domain;

public enum CourseOfferingStatus {
    /** Reusable blueprint; never open for registration. */
    TEMPLATE,
    DRAFT,
    PUBLISHED,
    /** Confirmed completion: results are final and the course has been credited. */
    COMPLETED,
    CANCELLED;

    /** Offering data (fields, sessions) may still be edited. */
    public boolean isEditable() {
        return this == TEMPLATE || this == DRAFT || this == PUBLISHED;
    }
}
