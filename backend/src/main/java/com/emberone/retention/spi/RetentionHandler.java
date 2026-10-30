/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.spi;

import com.emberone.retention.domain.RetentionCategory;
import java.time.Instant;
import java.util.Set;

public interface RetentionHandler {

    RetentionCategory category();

    /** Tables this handler owns; used by the coverage architecture test. */
    Set<String> tables();

    /**
     * Deletes (or, for {@link RetentionCategory#WORKFLOW_STALE}, closes) records whose trigger date
     * is before {@code cutoff}. Returns affected count.
     */
    int apply(Instant cutoff);
}
