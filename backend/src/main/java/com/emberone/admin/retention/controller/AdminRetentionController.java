/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.retention.controller;

import com.emberone.admin.retention.dto.TenantRetentionProfileDto;
import com.emberone.admin.retention.service.AdminRetentionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/admin/tenants/{tenantId}/retention")
@RequiredArgsConstructor
public class AdminRetentionController {

    private final AdminRetentionService adminRetentionService;

    @GetMapping
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantRetentionProfileDto> getRetentionProfile(
            @PathVariable long tenantId) {
        return ResponseEntity.ok(adminRetentionService.getProfileForTenant(tenantId));
    }
}
