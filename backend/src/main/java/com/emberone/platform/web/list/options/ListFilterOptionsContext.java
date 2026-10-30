/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list.options;

import com.emberone.permission.domain.PermissionType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resolution context for {@link ListFilterOptionsProvider}. {@code organizationId} is set for
 * {@link ListFilterOptionsScope#ORGANIZATION} (route org of the list). {@code
 * organizationFilterPermission} must match the list GET that consumes the filter (default {@link
 * PermissionType#USER_READ}).
 */
public record ListFilterOptionsContext(
        ListFilterOptionsScope scope,
        Long organizationId,
        PermissionType organizationFilterPermission) {

    public static ListFilterOptionsContext admin() {
        return new ListFilterOptionsContext(ListFilterOptionsScope.ADMIN, null, null);
    }

    public static ListFilterOptionsContext organization(long organizationId) {
        return new ListFilterOptionsContext(
                ListFilterOptionsScope.ORGANIZATION, organizationId, null);
    }

    public static ListFilterOptionsContext organization(
            long organizationId, PermissionType organizationFilterPermission) {
        return new ListFilterOptionsContext(
                ListFilterOptionsScope.ORGANIZATION, organizationId, organizationFilterPermission);
    }

    public PermissionType effectiveOrganizationFilterPermission() {
        return organizationFilterPermission != null
                ? organizationFilterPermission
                : PermissionType.USER_READ;
    }

    public long requireOrganizationId() {
        if (scope != ListFilterOptionsScope.ORGANIZATION || organizationId == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Missing organization context");
        }
        return organizationId;
    }
}
