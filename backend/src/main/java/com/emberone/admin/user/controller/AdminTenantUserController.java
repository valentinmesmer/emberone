/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.user.controller;

import com.emberone.admin.user.dto.PatchAdminUserAppRoleRequest;
import com.emberone.admin.user.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * User writes of the admin area. Tenant-scoped by path (RLS follows the path tenant), unlike the
 * cross-tenant read directory at {@code /api/v1/auth/admin/users}.
 */
@RestController
@RequestMapping("/api/v1/auth/admin/tenants/{tenantId}/users")
@RequiredArgsConstructor
public class AdminTenantUserController {

    private final AdminUserService adminUserService;

    @PatchMapping("/{userId}/app-role")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> patchUserAppRole(
            @PathVariable long tenantId,
            @PathVariable long userId,
            @Valid @RequestBody PatchAdminUserAppRoleRequest request) {
        adminUserService.updateUserAppRole(tenantId, userId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/deactivate")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable long tenantId, @PathVariable long userId) {
        adminUserService.deactivateUser(tenantId, userId);
        return ResponseEntity.noContent().build();
    }
}
