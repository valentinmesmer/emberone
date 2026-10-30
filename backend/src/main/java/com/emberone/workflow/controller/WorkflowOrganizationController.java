/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.controller;

import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.workflow.dto.WorkflowPageDto;
import com.emberone.workflow.list.WorkflowListFilter;
import com.emberone.workflow.list.WorkflowOrganizationListFilterOptions;
import com.emberone.workflow.service.WorkflowService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/workflows")
@RequiredArgsConstructor
public class WorkflowOrganizationController {

    private final WorkflowService workflowService;
    private final WorkflowOrganizationListFilterOptions organizationListFilterOptions;

    @GetMapping("/open/list-filter-options")
    @PreAuthorize("@securityService.hasWorkflowReadPermission(#organizationId)")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(organizationListFilterOptions.forOrganization(organizationId));
    }

    @GetMapping("/open")
    @PreAuthorize("@securityService.hasWorkflowReadPermission(#organizationId)")
    public ResponseEntity<WorkflowPageDto> listOpenWorkflows(
            @PathVariable Long organizationId,
            ListQuery query,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> type) {
        return ResponseEntity.ok(
                workflowService.listForOrganization(
                        organizationId, query, WorkflowListFilter.parse(status, type)));
    }
}
