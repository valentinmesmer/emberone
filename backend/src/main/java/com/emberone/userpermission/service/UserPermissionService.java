/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userpermission.service;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.userpermission.cache.UserPermissionCache;
import com.emberone.userpermission.dto.UserPermissionDto;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPermissionService {

    private final UserPermissionCache cachedUserPermissionLoader;
    private final TenantLocaleService tenantLocaleService;

    /**
     * Permission map of {@code userId} in the active tenant. Deliberately not transactional: on a
     * cache hit no connection is taken. The cache key and the RLS-scoped loader both bind the
     * active tenant, so an unknown or foreign user resolves to an empty map (fail closed).
     */
    public UserPermissionDto getUserPermissions(Long userId) {
        long tenantId = TenantScopeGuard.requireActiveTenantId();
        UserPermissionDto loaded = cachedUserPermissionLoader.load(tenantId, userId);
        SupportedLocale tenantLocale = tenantLocaleService.resolveTenantLocale(tenantId);
        Map<Long, List<PermissionType>> permissions = loaded.getPermissions();
        return UserPermissionDto.builder()
                .permissions(permissions == null ? Map.of() : permissions)
                .tenantLocale(tenantLocale)
                .build();
    }
}
