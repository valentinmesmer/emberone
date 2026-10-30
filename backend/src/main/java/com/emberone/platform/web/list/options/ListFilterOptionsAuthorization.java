/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list.options;

import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.platform.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Whether an organization (or tenant) may appear in list filter options. Must stay aligned with the
 * corresponding list GET authorization - options are never a broader set than list visibility.
 */
@Component
@RequiredArgsConstructor
public class ListFilterOptionsAuthorization {

    private final OrganizationAccessService organizationAccessService;
    private final TokenService tokenService;

    /** Admin user directory ({@code organizationId} filter). */
    public boolean mayExposeOrganizationForAdminUserListFilter(long organizationId) {
        return organizationAccessService.hasOrganizationPermission(
                organizationId, PermissionType.USER_READ);
    }

    /** Organization workspace lists ({@code organizationId} filter within a route org subtree). */
    public boolean mayExposeOrganizationForOrgWorkspaceFilter(
            long organizationId, PermissionType permissionType) {
        return organizationAccessService.hasOrganizationPermission(organizationId, permissionType);
    }

    public boolean mayExposeTenantForAdminUserListFilter() {
        return tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName());
    }
}
