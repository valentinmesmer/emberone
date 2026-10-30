/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.rank.controller;

import com.emberone.platform.web.list.BoundedList;
import com.emberone.rank.dto.*;
import com.emberone.rank.service.TenantRankCatalogService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/admin/tenants/{tenantId}/ranks")
@RequiredArgsConstructor
public class AdminTenantRankController {

    private final TenantRankCatalogService catalogService;

    @GetMapping
    @BoundedList("Tenant rank catalog")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<List<TenantRankDto>> list(@PathVariable long tenantId) {
        return ResponseEntity.ok(catalogService.listCatalog(tenantId));
    }

    @PostMapping
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantRankDto> createRank(
            @PathVariable long tenantId, @Valid @RequestBody CreateTenantRankRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogService.createRank(tenantId, request));
    }

    @PutMapping("/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantRankDto> updateRank(
            @PathVariable long tenantId,
            @PathVariable String configKey,
            @Valid @RequestBody UpdateTenantRankRequest request) {
        return ResponseEntity.ok(catalogService.updateRank(tenantId, configKey, request));
    }

    @PostMapping("/{configKey}/archive")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> archive(
            @PathVariable long tenantId, @PathVariable String configKey) {
        catalogService.archiveRank(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{configKey}/unarchive")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> unarchive(
            @PathVariable long tenantId, @PathVariable String configKey) {
        catalogService.unarchiveRank(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }
}
