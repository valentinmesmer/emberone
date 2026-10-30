/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Platform/admin role checks (global vs tenant admin, tenant scoping, self-service eligibility).
 */
@Service
@RequiredArgsConstructor
public class AdminAccessService {

    private final TokenService tokenService;

    public boolean isGlobalAdmin() {
        return tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName());
    }

    public boolean isTenantAdmin() {
        return tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName());
    }

    public boolean canListTenants() {
        return isGlobalAdmin() || isTenantAdmin();
    }

    public boolean canAccessTenant(long tenantId) {
        if (isGlobalAdmin()) {
            return true;
        }
        if (!isTenantAdmin()) {
            return false;
        }
        Long jwtTenantId = tokenService.getTenantId();
        return jwtTenantId != null && jwtTenantId == tenantId;
    }

    public boolean isAdminUser() {
        return isGlobalAdmin() || isTenantAdmin();
    }

    /**
     * Tenant users only. Platform admin ({@link EmberOneJwtClaims#PLATFORM_ADMIN_USER_ID}) has no
     * {@code users} row and must not call {@code /api/v1/self/**}.
     */
    public boolean isSelfServiceUser() {
        Long userId = tokenService.getUserId();
        return userId != null && userId != EmberOneJwtClaims.PLATFORM_ADMIN_USER_ID;
    }
}
