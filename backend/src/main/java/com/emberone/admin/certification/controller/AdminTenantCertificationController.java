/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.certification.controller;

import com.emberone.certification.dto.CreateTenantCertDefinitionRequest;
import com.emberone.certification.dto.TenantCertDefinitionDto;
import com.emberone.certification.dto.UpdateTenantCertDefinitionRequest;
import com.emberone.certification.service.TenantCertDefinitionService;
import com.emberone.platform.web.list.BoundedList;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/admin/tenants/{tenantId}/certification-definitions")
@RequiredArgsConstructor
public class AdminTenantCertificationController {

    private final TenantCertDefinitionService tenantCertDefinitionService;

    @GetMapping
    @BoundedList("Tenant certification definition catalog")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<List<TenantCertDefinitionDto>> list(@PathVariable long tenantId) {
        return ResponseEntity.ok(tenantCertDefinitionService.list(tenantId));
    }

    @GetMapping("/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantCertDefinitionDto> get(
            @PathVariable long tenantId, @PathVariable String configKey) {
        return ResponseEntity.ok(tenantCertDefinitionService.get(tenantId, configKey));
    }

    @PostMapping
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantCertDefinitionDto> create(
            @PathVariable long tenantId,
            @Valid @RequestBody CreateTenantCertDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tenantCertDefinitionService.create(tenantId, request));
    }

    @PutMapping("/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantCertDefinitionDto> update(
            @PathVariable long tenantId,
            @PathVariable String configKey,
            @Valid @RequestBody UpdateTenantCertDefinitionRequest request) {
        return ResponseEntity.ok(tenantCertDefinitionService.update(tenantId, configKey, request));
    }

    @DeleteMapping("/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> delete(
            @PathVariable long tenantId, @PathVariable String configKey) {
        tenantCertDefinitionService.delete(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{configKey}/archive")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> archive(
            @PathVariable long tenantId, @PathVariable String configKey) {
        tenantCertDefinitionService.archive(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{configKey}/unarchive")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> unarchive(
            @PathVariable long tenantId, @PathVariable String configKey) {
        tenantCertDefinitionService.unarchive(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }
}
