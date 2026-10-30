/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.controller;

import com.emberone.admin.tenant.dto.CreateTenantRequest;
import com.emberone.admin.tenant.dto.PurgeTenantRequest;
import com.emberone.admin.tenant.dto.TenantAdminDetailDto;
import com.emberone.admin.tenant.dto.TenantAdminPatchRequest;
import com.emberone.admin.tenant.dto.TenantDto;
import com.emberone.admin.tenant.list.AdminTenantListFilterOptions;
import com.emberone.admin.tenant.list.TenantListFilter;
import com.emberone.admin.tenant.service.AdminTenantPurgeService;
import com.emberone.admin.tenant.service.AdminTenantService;
import com.emberone.federalstate.domain.FederalState;
import com.emberone.organization.dto.SetupOrganizationRequest;
import com.emberone.platform.web.PageDto;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/admin/tenants")
@RequiredArgsConstructor
public class AdminTenantController {

    private final AdminTenantService adminTenantService;
    private final AdminTenantListFilterOptions adminTenantListFilterOptions;
    private final AdminTenantPurgeService adminTenantPurgeService;

    @GetMapping("/list-filter-options")
    @PreAuthorize("@securityService.canListTenants()")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions() {
        return ResponseEntity.ok(adminTenantListFilterOptions.filterOptions());
    }

    @GetMapping
    @PreAuthorize("@securityService.canListTenants()")
    public ResponseEntity<PageDto<TenantDto>> listTenants(
            ListQuery query, @RequestParam(required = false) List<String> federalState) {
        return ResponseEntity.ok(
                adminTenantService.listTenants(query, TenantListFilter.parse(federalState)));
    }

    @GetMapping("/federal-states")
    @BoundedList("Configured federal states for tenant creation")
    @PreAuthorize("@securityService.isGlobalAdmin()")
    public ResponseEntity<List<FederalState>> listConfiguredFederalStates() {
        return ResponseEntity.ok(adminTenantService.listConfiguredFederalStates());
    }

    @PostMapping
    @PreAuthorize("@securityService.isGlobalAdmin()")
    public ResponseEntity<TenantDto> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminTenantService.createTenant(request));
    }

    @GetMapping("/{tenantId}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantAdminDetailDto> getTenantDetail(@PathVariable long tenantId) {
        return ResponseEntity.ok(adminTenantService.getTenantDetail(tenantId));
    }

    @PatchMapping("/{tenantId}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantAdminDetailDto> patchTenant(
            @PathVariable long tenantId, @Valid @RequestBody TenantAdminPatchRequest request) {
        return ResponseEntity.ok(adminTenantService.patchTenant(tenantId, request));
    }

    @PostMapping("/{tenantId}/root-organization")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantAdminDetailDto> createRootOrganization(
            @PathVariable long tenantId, @Valid @RequestBody SetupOrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminTenantService.createRootOrganization(tenantId, request));
    }

    @PostMapping("/{tenantId}/baseline-sync")
    @PreAuthorize("@securityService.isGlobalAdmin()")
    public ResponseEntity<Void> syncBaseline(@PathVariable long tenantId) {
        adminTenantService.syncBaseline(tenantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tenantId}/purge")
    @PreAuthorize("@securityService.isGlobalAdmin()")
    public ResponseEntity<Void> purgeTenant(
            @PathVariable long tenantId, @Valid @RequestBody PurgeTenantRequest request) {
        adminTenantPurgeService.purgeTenant(tenantId, request);
        return ResponseEntity.noContent().build();
    }
}
