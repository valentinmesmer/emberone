/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.controller;

import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.user.service.UserService;
import com.emberone.workflow.dto.WorkflowPageDto;
import com.emberone.workflow.list.WorkflowListFilter;
import com.emberone.workflow.list.WorkflowUserOrganizationListFilterOptions;
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
@RequestMapping("/api/v1/auth/organizations/{organizationId}/users/{userId}/workflows")
@RequiredArgsConstructor
public class WorkflowUserOrganizationController {

    private final WorkflowService workflowService;
    private final UserService userService;
    private final WorkflowUserOrganizationListFilterOptions listFilterOptions;

    @GetMapping("/list-filter-options")
    @PreAuthorize("@securityService.hasWorkflowReadPermission(#organizationId)")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(listFilterOptions.forUserInOrganization(userId, organizationId));
    }

    @GetMapping
    @PreAuthorize("@securityService.hasWorkflowReadPermission(#organizationId)")
    public ResponseEntity<WorkflowPageDto> listWorkflowsOpenedByUser(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            ListQuery query,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> type) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(
                workflowService.listOpenedByUserInOrganization(
                        userId, organizationId, query, WorkflowListFilter.parse(status, type)));
    }
}
