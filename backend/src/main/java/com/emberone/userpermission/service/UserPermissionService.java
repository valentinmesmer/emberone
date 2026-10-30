/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userpermission.service;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.user.domain.User;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.userpermission.cache.CachedUserPermissionLoader;
import com.emberone.userpermission.dto.UserPermissionDto;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPermissionService {

    private final CachedUserPermissionLoader cachedUserPermissionLoader;
    private final UserRepository userRepository;
    private final TenantLocaleService tenantLocaleService;

    @Transactional(readOnly = true)
    public UserPermissionDto getUserPermissions(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        TenantScopeGuard.requireEntityInActiveTenant(user.getTenantId(), "User", userId);

        UserPermissionDto loaded = cachedUserPermissionLoader.load(user.getTenantId(), userId);
        SupportedLocale tenantLocale = tenantLocaleService.resolveTenantLocale(user.getTenantId());
        Map<Long, List<PermissionType>> permissions = loaded.getPermissions();
        return UserPermissionDto.builder()
                .permissions(permissions == null ? Map.of() : permissions)
                .tenantLocale(tenantLocale)
                .build();
    }
}
