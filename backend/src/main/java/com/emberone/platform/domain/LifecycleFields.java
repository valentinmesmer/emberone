/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.domain;

import java.time.Instant;

/**
 * Shared semantics for {@code active}, {@code archived}/{@code archived_at}, and related fields.
 * See support/docs/concept/lifecycle-and-status-fields.md.
 */
public final class LifecycleFields {

    private LifecycleFields() {}

    /** Entity with a soft-retirement flag and audit timestamp (no separate {@code active}). */
    public interface Archivable {

        boolean isArchived();

        void setArchived(boolean archived);

        Instant getArchivedAt();

        void setArchivedAt(Instant archivedAt);
    }

    /**
     * Tenant catalog row: admin toggle {@code active} plus optional archive when history blocks
     * delete.
     */
    public interface ActivatableCatalogEntry extends Archivable {

        boolean isActive();

        void setActive(boolean active);
    }

    public static void archive(Archivable target, Instant now) {
        target.setArchived(true);
        target.setArchivedAt(now);
    }

    public static void archiveCatalogEntry(ActivatableCatalogEntry target, Instant now) {
        archive(target, now);
        target.setActive(false);
    }

    public static void unarchiveCatalogEntry(ActivatableCatalogEntry target, boolean active) {
        target.setArchived(false);
        target.setArchivedAt(null);
        target.setActive(active);
    }

    /** Whether the catalog entry may be selected for new assignments, workflows, or standards. */
    public static boolean isCatalogSelectable(boolean active, boolean archived) {
        return active && !archived;
    }

    public static boolean isCatalogSelectable(ActivatableCatalogEntry entry) {
        return isCatalogSelectable(entry.isActive(), entry.isArchived());
    }
}
