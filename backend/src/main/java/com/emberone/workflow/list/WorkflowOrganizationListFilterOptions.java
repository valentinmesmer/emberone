/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.list;

import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.workflow.service.WorkflowService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Filter options for {@code GET /api/v1/auth/organizations/{organizationId}/workflows/open}. */
@Component
@RequiredArgsConstructor
public class WorkflowOrganizationListFilterOptions {

    private final WorkflowService workflowService;
    private final WorkflowTypeFilterOptionsMapper typeFilterOptionsMapper;

    public ListFilterOptionsDto forOrganization(long organizationId) {
        return new ListFilterOptionsDto(
                Map.of(
                        "type",
                        typeFilterOptionsMapper.toTypeOptions(
                                workflowService.listDistinctWorkflowTypeIdsForOrganization(
                                        organizationId))));
    }
}
