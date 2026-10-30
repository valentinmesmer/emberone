/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.spi.RetentionHandler;
import java.time.Instant;
import java.util.Set;

/** Test double: a handler that deletes nothing. */
public final class NoOpRetentionHandler implements RetentionHandler {

    private final RetentionCategory category;
    private final Set<String> tables;

    public NoOpRetentionHandler(RetentionCategory category, Set<String> tables) {
        this.category = category;
        this.tables = Set.copyOf(tables);
    }

    @Override
    public RetentionCategory category() {
        return category;
    }

    @Override
    public Set<String> tables() {
        return tables;
    }

    @Override
    public int apply(Instant cutoff) {
        return 0;
    }
}
