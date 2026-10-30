/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.user.list;

import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.web.list.ListFilters;
import java.util.List;

/**
 * Optional filters of the admin user directory. Repeated {@code tenantId}, {@code organizationId}
 * and {@code appRole} query params are combined with OR inside each dimension; dimensions together
 * use AND. Values are validated; absent/empty lists mean "no restriction".
 */
public record AdminUserListFilter(
        List<Long> tenantIds, List<Long> organizationIds, List<EmberOneAppRole> appRoles) {

    public static final AdminUserListFilter NONE =
            new AdminUserListFilter(List.of(), List.of(), List.of());

    /** Parses request parameters. */
    public static AdminUserListFilter parse(
            List<Long> tenantIds, List<Long> organizationIds, List<String> appRole) {
        return new AdminUserListFilter(
                ListFilters.optionalLongIds(tenantIds),
                ListFilters.optionalLongIds(organizationIds),
                ListFilters.parseEnumList(appRole, "appRole", EmberOneAppRole::fromRoleName));
    }
}
