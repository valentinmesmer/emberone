/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.list;

import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.platform.web.list.options.ListFilterOptionsSource;
import com.emberone.workflow.service.WorkflowService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Filter options for self workflow inbox lists ({@code
 * /api/v1/self/workflows/opened|participations}).
 */
@Component
@RequiredArgsConstructor
public class WorkflowSelfListFilterOptions implements ListFilterOptionsSource {

    private final WorkflowService workflowService;
    private final WorkflowTypeFilterOptionsMapper typeFilterOptionsMapper;

    @Override
    @Transactional(readOnly = true)
    public ListFilterOptionsDto filterOptions() {
        return new ListFilterOptionsDto(
                Map.of(
                        "type",
                        typeFilterOptionsMapper.toTypeOptions(
                                workflowService.listDistinctInboxWorkflowTypeIds())));
    }
}
