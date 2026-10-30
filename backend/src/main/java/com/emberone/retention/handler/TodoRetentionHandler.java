/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.repository.TodoRetentionRepository;
import com.emberone.retention.spi.RetentionHandler;
import com.emberone.retention.support.RetentionBatchDeletes;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TodoRetentionHandler implements RetentionHandler {

    private static final Set<String> TABLES = Set.of("todo_items", "todo_excluded_users");

    private final TodoRetentionRepository todoRetentionRepository;

    @Override
    public RetentionCategory category() {
        return RetentionCategory.TODO;
    }

    @Override
    public Set<String> tables() {
        return TABLES;
    }

    @Override
    public int apply(Instant cutoff) {
        return RetentionBatchDeletes.deleteUntilExhausted(
                () -> todoRetentionRepository.deleteArchivedTodosBefore(cutoff));
    }
}
