/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.list;

import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.workflow.service.WorkflowService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Filter options for org user-management workflow lists. */
@Component
@RequiredArgsConstructor
public class WorkflowUserOrganizationListFilterOptions {

    private final WorkflowService workflowService;
    private final WorkflowTypeFilterOptionsMapper typeFilterOptionsMapper;

    public ListFilterOptionsDto forUserInOrganization(long userId, long organizationId) {
        return new ListFilterOptionsDto(
                Map.of(
                        "type",
                        typeFilterOptionsMapper.toTypeOptions(
                                workflowService
                                        .listDistinctWorkflowTypeIdsOpenedByUserInOrganization(
                                                userId, organizationId))));
    }
}
