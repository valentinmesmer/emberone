/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.domain;

/** Who may register for an offering. An empty audience list means public (whole tenant). */
public enum CourseAudienceKind {
    /** Members of the offering organization only. Must be the only entry (private offering). */
    OWN_ORG(false, true),
    /** Members of one organization. */
    ORG(true, false),
    /** Members of any organization of one organization type. */
    ORG_TYPE(true, false),
    /** Members of one organization or any of its descendants. */
    ORG_SUBTREE(true, false);

    private final boolean needsReference;
    private final boolean exclusive;

    CourseAudienceKind(boolean needsReference, boolean exclusive) {
        this.needsReference = needsReference;
        this.exclusive = exclusive;
    }

    public boolean needsReference() {
        return needsReference;
    }

    /** An exclusive kind must be the only audience entry of an offering. */
    public boolean isExclusive() {
        return exclusive;
    }
}
