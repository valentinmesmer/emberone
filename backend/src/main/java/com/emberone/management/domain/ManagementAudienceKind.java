/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.domain;

/** Which organizations of the tenant a managing organization may open. */
public enum ManagementAudienceKind {
    /** One organization. */
    ORG(true),
    /** One organization and all its descendants. */
    ORG_SUBTREE(true),
    /** Every organization of one organization type. */
    ORG_TYPE(true),
    /** Every organization of the tenant. */
    TENANT(false);

    private final boolean needsReference;

    ManagementAudienceKind(boolean needsReference) {
        this.needsReference = needsReference;
    }

    public boolean needsReference() {
        return needsReference;
    }
}
