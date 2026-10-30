/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class LifecycleFieldsTest {

    @Test
    void archiveCatalogEntryClearsActiveAndSetsTimestamp() {
        StubCatalog entry = new StubCatalog(true, false, null);
        Instant now = Instant.parse("2026-01-15T12:00:00Z");

        LifecycleFields.archiveCatalogEntry(entry, now);

        assertThat(entry.archived).isTrue();
        assertThat(entry.archivedAt).isEqualTo(now);
        assertThat(entry.active).isFalse();
    }

    @Test
    void unarchiveCatalogEntryRestoresActiveFlag() {
        StubCatalog entry = new StubCatalog(false, true, Instant.now());

        LifecycleFields.unarchiveCatalogEntry(entry, true);

        assertThat(entry.archived).isFalse();
        assertThat(entry.archivedAt).isNull();
        assertThat(entry.active).isTrue();
    }

    @Test
    void isCatalogSelectableRequiresActiveAndNotArchived() {
        assertThat(LifecycleFields.isCatalogSelectable(true, false)).isTrue();
        assertThat(LifecycleFields.isCatalogSelectable(false, false)).isFalse();
        assertThat(LifecycleFields.isCatalogSelectable(true, true)).isFalse();
    }

    private static final class StubCatalog implements LifecycleFields.ActivatableCatalogEntry {

        private boolean active;
        private boolean archived;
        private Instant archivedAt;

        private StubCatalog(boolean active, boolean archived, Instant archivedAt) {
            this.active = active;
            this.archived = archived;
            this.archivedAt = archivedAt;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void setActive(boolean active) {
            this.active = active;
        }

        @Override
        public boolean isArchived() {
            return archived;
        }

        @Override
        public void setArchived(boolean archived) {
            this.archived = archived;
        }

        @Override
        public Instant getArchivedAt() {
            return archivedAt;
        }

        @Override
        public void setArchivedAt(Instant archivedAt) {
            this.archivedAt = archivedAt;
        }
    }
}
