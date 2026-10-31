/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
