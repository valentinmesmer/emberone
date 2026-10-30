/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.drivinglicense.controller;

import com.emberone.drivinglicense.dto.CreateTenantDrivingLicenseDefinitionRequest;
import com.emberone.drivinglicense.dto.TenantDrivingLicenseDefinitionDto;
import com.emberone.drivinglicense.dto.UpdateTenantDrivingLicenseDefinitionRequest;
import com.emberone.drivinglicense.service.TenantDrivingLicenseDefinitionService;
import com.emberone.platform.web.list.BoundedList;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/admin/tenants/{tenantId}/driving-license-definitions")
@RequiredArgsConstructor
public class AdminTenantDrivingLicenseController {

    private final TenantDrivingLicenseDefinitionService tenantDrivingLicenseDefinitionService;

    @GetMapping
    @BoundedList("Tenant driving license definition catalog")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<List<TenantDrivingLicenseDefinitionDto>> list(
            @PathVariable long tenantId) {
        return ResponseEntity.ok(tenantDrivingLicenseDefinitionService.list(tenantId));
    }

    @GetMapping("/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantDrivingLicenseDefinitionDto> get(
            @PathVariable long tenantId, @PathVariable String configKey) {
        return ResponseEntity.ok(tenantDrivingLicenseDefinitionService.get(tenantId, configKey));
    }

    @PostMapping
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantDrivingLicenseDefinitionDto> create(
            @PathVariable long tenantId,
            @Valid @RequestBody CreateTenantDrivingLicenseDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tenantDrivingLicenseDefinitionService.create(tenantId, request));
    }

    @PutMapping("/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantDrivingLicenseDefinitionDto> update(
            @PathVariable long tenantId,
            @PathVariable String configKey,
            @Valid @RequestBody UpdateTenantDrivingLicenseDefinitionRequest request) {
        return ResponseEntity.ok(
                tenantDrivingLicenseDefinitionService.update(tenantId, configKey, request));
    }

    @DeleteMapping("/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> delete(
            @PathVariable long tenantId, @PathVariable String configKey) {
        tenantDrivingLicenseDefinitionService.delete(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{configKey}/archive")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> archive(
            @PathVariable long tenantId, @PathVariable String configKey) {
        tenantDrivingLicenseDefinitionService.archive(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{configKey}/unarchive")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> unarchive(
            @PathVariable long tenantId, @PathVariable String configKey) {
        tenantDrivingLicenseDefinitionService.unarchive(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }
}
