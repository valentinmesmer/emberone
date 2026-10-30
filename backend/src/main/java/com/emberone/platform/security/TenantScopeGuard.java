/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import com.emberone.platform.exception.ResourceNotFoundException;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Application-level tenant isolation. Complements PostgreSQL RLS ({@code app.tenant_id}) so tenant
 * scoping still holds when the DB session context is missing or stale (e.g. global admin without
 * {@code tenantId} in the JWT, as long as the admin path interceptor bound the target tenant).
 */
@UtilityClass
public class TenantScopeGuard {

    /** Bind the active tenant for the current request and return it. */
    public static long bind(long tenantId) {
        TenantContextHolder.setTenantId(tenantId);
        return requireActiveTenantId();
    }

    public static long requireActiveTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant in context");
        }
        return tenantId;
    }

    /** Ensure the bound tenant matches the tenant id from the admin API path. */
    public static void requireActiveTenantMatches(long expectedTenantId) {
        if (requireActiveTenantId() != expectedTenantId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant scope mismatch");
        }
    }

    /**
     * Reject cross-tenant entity access without revealing that the row exists in another tenant.
     */
    public static void requireEntityInActiveTenant(
            Long entityTenantId, String resourceLabel, Object resourceId) {
        long activeTenantId = requireActiveTenantId();
        if (entityTenantId == null || !entityTenantId.equals(activeTenantId)) {
            throw new ResourceNotFoundException(resourceLabel + " not found: " + resourceId);
        }
    }
}
