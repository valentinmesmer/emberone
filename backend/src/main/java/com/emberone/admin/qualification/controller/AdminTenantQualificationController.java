/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.qualification.controller;

import com.emberone.platform.web.list.BoundedList;
import com.emberone.qualification.dto.CreateTenantQualificationDefinitionRequest;
import com.emberone.qualification.dto.TenantQualificationDefinitionDto;
import com.emberone.qualification.dto.UpdateTenantQualificationDefinitionRequest;
import com.emberone.qualification.service.TenantQualificationDefinitionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/admin/tenants/{tenantId}/qualification-definitions")
@RequiredArgsConstructor
public class AdminTenantQualificationController {

    private final TenantQualificationDefinitionService definitionService;

    @GetMapping
    @BoundedList("Tenant qualification definition catalog")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<List<TenantQualificationDefinitionDto>> list(
            @PathVariable long tenantId) {
        return ResponseEntity.ok(definitionService.list(tenantId));
    }

    @PostMapping
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantQualificationDefinitionDto> create(
            @PathVariable long tenantId,
            @Valid @RequestBody CreateTenantQualificationDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(definitionService.create(tenantId, request));
    }

    @PutMapping("/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantQualificationDefinitionDto> update(
            @PathVariable long tenantId,
            @PathVariable String configKey,
            @Valid @RequestBody UpdateTenantQualificationDefinitionRequest request) {
        return ResponseEntity.ok(definitionService.update(tenantId, configKey, request));
    }

    @DeleteMapping("/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> delete(
            @PathVariable long tenantId, @PathVariable String configKey) {
        definitionService.delete(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{configKey}/archive")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> archive(
            @PathVariable long tenantId, @PathVariable String configKey) {
        definitionService.archive(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{configKey}/unarchive")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> unarchive(
            @PathVariable long tenantId, @PathVariable String configKey) {
        definitionService.unarchive(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }
}
