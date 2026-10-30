/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.organization.controller;

import com.emberone.admin.organization.dto.*;
import com.emberone.admin.organization.service.AdminOrganizationService;
import com.emberone.organization.dto.CreateChildOrganizationRequest;
import com.emberone.organization.dto.DeactivateOrganizationRequest;
import com.emberone.organization.dto.MoveOrganizationRequest;
import com.emberone.organization.dto.UpdateOrganizationRequest;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.user.dto.CreateUserRequest;
import com.emberone.user.dto.UserPageDto;
import com.emberone.userorganizationgroupmembership.dto.UpdateUserOrganizationGroupMembershipRequest;
import com.emberone.userorganizationgroupmembership.dto.UserOrganizationGroupMembershipDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/admin/tenants/{tenantId}/organizations")
@RequiredArgsConstructor
public class AdminOrganizationController {

    private final AdminOrganizationService adminOrganizationService;

    @GetMapping("/tree")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<AdminOrganizationTreeResponseDto> getOrganizationTree(
            @PathVariable long tenantId) {
        return ResponseEntity.ok(adminOrganizationService.getOrganizationTree(tenantId));
    }

    @GetMapping("/{organizationId}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<AdminOrganizationDetailDto> getOrganization(
            @PathVariable long tenantId, @PathVariable long organizationId) {
        return ResponseEntity.ok(
                adminOrganizationService.getOrganization(tenantId, organizationId));
    }

    @GetMapping("/{organizationId}/users")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<UserPageDto> getOrganizationUsers(
            @PathVariable long tenantId, @PathVariable long organizationId, ListQuery query) {
        return ResponseEntity.ok(
                adminOrganizationService.getOrganizationUsers(tenantId, organizationId, query));
    }

    @GetMapping("/{organizationId}/children")
    @BoundedList("Direct child organizations of one organization; configuration list")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<List<AdminOrganizationDetailDto>> getChildOrganizations(
            @PathVariable long tenantId, @PathVariable long organizationId) {
        return ResponseEntity.ok(
                adminOrganizationService.getChildOrganizations(tenantId, organizationId));
    }

    @PostMapping("/{organizationId}/children")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<AdminOrganizationDetailDto> createChildOrganization(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @Valid @RequestBody CreateChildOrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        adminOrganizationService.createChildOrganization(
                                tenantId, organizationId, request));
    }

    @PostMapping("/{organizationId}/move")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<AdminOrganizationDetailDto> moveOrganization(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @Valid @RequestBody MoveOrganizationRequest request) {
        return ResponseEntity.ok(
                adminOrganizationService.moveOrganization(tenantId, organizationId, request));
    }

    @PostMapping("/{organizationId}/deactivate")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> deactivateOrganization(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @Valid @RequestBody DeactivateOrganizationRequest request) {
        adminOrganizationService.deactivateOrganization(tenantId, organizationId, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{organizationId}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<AdminOrganizationDetailDto> updateOrganization(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        return ResponseEntity.ok(
                adminOrganizationService.updateOrganization(tenantId, organizationId, request));
    }

    @PostMapping("/{organizationId}/leader-user")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<AdminOrganizationDetailDto> createOrganizationLeaderUser(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        adminOrganizationService.createOrganizationLeaderUser(
                                tenantId, organizationId, request));
    }

    @GetMapping("/{organizationId}/leadership-group-memberships")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<AdminLeadershipGroupMembershipsDto> getLeadershipGroupMemberships(
            @PathVariable long tenantId, @PathVariable long organizationId) {
        return ResponseEntity.ok(
                adminOrganizationService.getLeadershipGroupMemberships(tenantId, organizationId));
    }

    @PostMapping("/{organizationId}/leadership-group-memberships")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<UserOrganizationGroupMembershipDto> createLeadershipGroupMembership(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @Valid @RequestBody CreateAdminLeadershipGroupMembershipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        adminOrganizationService.createLeadershipGroupMembership(
                                tenantId, organizationId, request));
    }

    @PutMapping("/{organizationId}/leadership-group-memberships/{membershipId}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<UserOrganizationGroupMembershipDto> updateLeadershipGroupMembership(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long membershipId,
            @Valid @RequestBody UpdateUserOrganizationGroupMembershipRequest request) {
        return ResponseEntity.ok(
                adminOrganizationService.updateLeadershipGroupMembership(
                        tenantId, organizationId, membershipId, request));
    }
}
