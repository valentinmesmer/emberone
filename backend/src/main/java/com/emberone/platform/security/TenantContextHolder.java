/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TenantContextHolder {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> GLOBAL_ADMIN_READ = new ThreadLocal<>();

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    /**
     * Whether this request may read across tenants (global admin on a cross-tenant admin route, see
     * {@link GlobalAdminReadScopeInterceptor}). Applied as GUC {@code app.global_admin}; only
     * SELECT policies honor it.
     */
    public static boolean isGlobalAdminRead() {
        return Boolean.TRUE.equals(GLOBAL_ADMIN_READ.get());
    }

    public static void setGlobalAdminRead(boolean enabled) {
        if (enabled) {
            GLOBAL_ADMIN_READ.set(true);
        } else {
            GLOBAL_ADMIN_READ.remove();
        }
    }

    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
        GLOBAL_ADMIN_READ.remove();
    }
}
