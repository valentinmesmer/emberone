/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.management.controller;

import com.emberone.admin.management.list.AdminManagementActivationListFilterOptions;
import com.emberone.management.dto.ManagementActivationLogItemDto;
import com.emberone.management.list.ManagementActivationListFilter;
import com.emberone.management.service.ManagementActivationLogService;
import com.emberone.platform.web.PageDto;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Cross-tenant read log for global admins; tenant admins see their own tenant. */
@RestController
@RequestMapping("/api/v1/auth/admin/management-activations")
@RequiredArgsConstructor
public class AdminManagementActivationController {

    private final ManagementActivationLogService logService;
    private final AdminManagementActivationListFilterOptions filterOptions;

    @GetMapping("/list-filter-options")
    @PreAuthorize("@securityService.isAdminUser()")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions() {
        return ResponseEntity.ok(filterOptions.filterOptions());
    }

    @GetMapping("/{activationId}")
    @PreAuthorize("@securityService.isAdminUser()")
    public ResponseEntity<ManagementActivationLogItemDto> get(@PathVariable long activationId) {
        return ResponseEntity.ok(logService.getForAdmin(activationId));
    }

    @GetMapping
    @PreAuthorize("@securityService.isAdminUser()")
    public ResponseEntity<PageDto<ManagementActivationLogItemDto>> list(
            ListQuery query,
            @RequestParam(required = false) List<Long> tenantId,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<Long> organizationId) {
        return ResponseEntity.ok(
                logService.listForAdmin(
                        query,
                        ManagementActivationListFilter.parse(
                                tenantId, status, organizationId, List.of())));
    }
}
