/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.organizationtype.controller;

import com.emberone.admin.organizationtype.dto.CreateOrganizationTypeRequest;
import com.emberone.admin.organizationtype.dto.OrganizationTypeDto;
import com.emberone.admin.organizationtype.dto.UpdateOrganizationTypeRequest;
import com.emberone.admin.organizationtype.service.AdminOrganizationTypeService;
import com.emberone.platform.web.list.BoundedList;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Tenant-scoped CRUD for the organization type catalog. */
@RestController
@RequestMapping("/api/v1/auth/admin/tenants/{tenantId}/organization-types")
@RequiredArgsConstructor
public class AdminOrganizationTypeController {

    private final AdminOrganizationTypeService adminOrganizationTypeService;

    @GetMapping
    @BoundedList("Tenant organization type catalog")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<List<OrganizationTypeDto>> listTypes(@PathVariable long tenantId) {
        return ResponseEntity.ok(adminOrganizationTypeService.listTypes(tenantId));
    }

    @PostMapping
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<OrganizationTypeDto> createType(
            @PathVariable long tenantId,
            @Valid @RequestBody CreateOrganizationTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminOrganizationTypeService.createType(tenantId, request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<OrganizationTypeDto> updateType(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrganizationTypeRequest request) {
        return ResponseEntity.ok(adminOrganizationTypeService.updateType(tenantId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> deleteType(@PathVariable long tenantId, @PathVariable Long id) {
        adminOrganizationTypeService.deleteType(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
