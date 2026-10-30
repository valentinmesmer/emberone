/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.management.controller;

import com.emberone.management.dto.ManagementAudienceSettingsDto;
import com.emberone.management.dto.ReplaceManagementAudiencesRequest;
import com.emberone.management.service.ManagementAudienceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/v1/auth/admin/tenants/{tenantId}/organizations/{organizationId}/management-audiences")
@RequiredArgsConstructor
public class AdminManagementAudienceController {

    private final ManagementAudienceService managementAudienceService;

    @GetMapping
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<ManagementAudienceSettingsDto> getManagementAudiences(
            @PathVariable long tenantId, @PathVariable long organizationId) {
        return ResponseEntity.ok(managementAudienceService.getSettings(tenantId, organizationId));
    }

    @PutMapping
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<ManagementAudienceSettingsDto> replaceManagementAudiences(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @Valid @RequestBody ReplaceManagementAudiencesRequest request) {
        return ResponseEntity.ok(
                managementAudienceService.replace(tenantId, organizationId, request.getEntries()));
    }
}
