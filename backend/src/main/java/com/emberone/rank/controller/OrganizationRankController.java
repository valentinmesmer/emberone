/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.controller;

import com.emberone.dutyfunction.dto.OrganizationFunctionOverviewDto;
import com.emberone.dutyfunction.service.DutyFunctionResolutionService;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.rank.dto.OrganizationRankMemberPageDto;
import com.emberone.rank.dto.TenantRankDto;
import com.emberone.rank.service.TenantRankCatalogService;
import com.emberone.rank.service.UserRankService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/ranks")
@RequiredArgsConstructor
public class OrganizationRankController {

    private final UserRankService userRankService;
    private final TenantRankCatalogService tenantRankCatalogService;
    private final DutyFunctionResolutionService dutyFunctionResolutionService;

    @GetMapping
    @PreAuthorize("@securityService.hasRankReadPermission(#organizationId)")
    public ResponseEntity<OrganizationRankMemberPageDto> listMembers(
            @PathVariable Long organizationId, ListQuery query) {
        return ResponseEntity.ok(userRankService.listOrganizationMembers(organizationId, query));
    }

    @GetMapping("/available-ranks")
    @BoundedList("Tenant rank catalog for assignment UI")
    @PreAuthorize("@securityService.hasRankWritePermission(#organizationId)")
    public ResponseEntity<List<TenantRankDto>> listAvailableRanks(
            @PathVariable Long organizationId) {
        long tenantId = TenantScopeGuard.requireActiveTenantId();
        return ResponseEntity.ok(tenantRankCatalogService.listActiveRanks(tenantId));
    }

    @GetMapping("/functions/overview")
    @PreAuthorize("@securityService.hasRankReadPermission(#organizationId)")
    public ResponseEntity<OrganizationFunctionOverviewDto> getFunctionsOverview(
            @PathVariable Long organizationId, @RequestParam(required = false) String search) {
        return ResponseEntity.ok(
                dutyFunctionResolutionService.getOrganizationFunctionOverview(
                        organizationId, search));
    }
}
