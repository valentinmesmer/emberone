/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.list;

import com.emberone.platform.web.list.ListFilters;
import com.emberone.workflow.domain.WorkflowStatus;
import java.util.List;
import java.util.Optional;

/**
 * Workflow list filters. Repeated {@code status} and {@code type} (workflow type id) params are
 * OR-combined within each dimension; empty {@code status} means no status restriction.
 */
public record WorkflowListFilter(List<WorkflowStatus> statuses, List<Long> workflowTypeIds) {

    public WorkflowListFilter {
        if (statuses == null) {
            statuses = List.of();
        }
        if (workflowTypeIds == null) {
            workflowTypeIds = List.of();
        }
    }

    public static WorkflowListFilter parse(List<String> status, List<String> type) {
        return new WorkflowListFilter(
                parseStatuses(status), ListFilters.parseLongList(type, "type"));
    }

    private static List<WorkflowStatus> parseStatuses(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return ListFilters.parseEnumList(
                raw,
                "status",
                value -> {
                    try {
                        return Optional.of(WorkflowStatus.valueOf(value));
                    } catch (IllegalArgumentException ex) {
                        return Optional.empty();
                    }
                });
    }
}
