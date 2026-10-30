/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.dutyfunction.controller;

import com.emberone.dutyfunction.dto.*;
import com.emberone.dutyfunction.service.TenantDutyFunctionDefinitionService;
import com.emberone.platform.web.list.BoundedList;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/admin/tenants/{tenantId}/functions")
@RequiredArgsConstructor
public class AdminTenantDutyFunctionController {

    private final TenantDutyFunctionDefinitionService functionService;

    @GetMapping
    @BoundedList("Tenant duty function catalog")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<List<TenantFunctionDto>> list(@PathVariable long tenantId) {
        return ResponseEntity.ok(functionService.list(tenantId));
    }

    @PostMapping
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantFunctionDto> create(
            @PathVariable long tenantId, @Valid @RequestBody CreateTenantFunctionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(functionService.create(tenantId, request));
    }

    @PutMapping("/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantFunctionDto> update(
            @PathVariable long tenantId,
            @PathVariable String configKey,
            @Valid @RequestBody UpdateTenantFunctionRequest request) {
        return ResponseEntity.ok(functionService.update(tenantId, configKey, request));
    }

    @PostMapping("/{configKey}/archive")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> archive(
            @PathVariable long tenantId, @PathVariable String configKey) {
        functionService.archive(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{configKey}/unarchive")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> unarchive(
            @PathVariable long tenantId, @PathVariable String configKey) {
        functionService.unarchive(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }
}
