/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.controller;

import com.emberone.management.dto.ManagementActivationLogItemDto;
import com.emberone.management.list.ManagementActivationListFilter;
import com.emberone.management.service.ManagementActivationLogService;
import com.emberone.platform.web.PageDto;
import com.emberone.platform.web.list.ListQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/management-activations")
@RequiredArgsConstructor
public class OrganizationManagementActivationController {

    private final ManagementActivationLogService logService;

    /** Who managed this organization, when and why. */
    @GetMapping
    @PreAuthorize(
            "@securityService.hasOrganizationPermission(#organizationId,"
                    + " T(com.emberone.permission.domain.PermissionType).ORGANIZATION_READ)")
    public ResponseEntity<PageDto<ManagementActivationLogItemDto>> list(
            @PathVariable long organizationId,
            ListQuery query,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<Long> managingOrganizationId) {
        return ResponseEntity.ok(
                logService.listForTarget(
                        organizationId,
                        query,
                        ManagementActivationListFilter.parse(
                                List.of(), status, List.of(), managingOrganizationId)));
    }
}
