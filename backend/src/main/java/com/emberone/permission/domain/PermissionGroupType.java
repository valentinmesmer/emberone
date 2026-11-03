/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.permission.domain;

import java.util.Arrays;
import java.util.List;

public enum PermissionGroupType {
    ADMIN,
    STANDARD,
    CUSTOM;

    public boolean isSystem() {
        return this != CUSTOM;
    }

    public String getMessageKey() {
        return "permissionGroup." + name();
    }

    /**
     * ADMIN receives every {@link PermissionType}; materialization then clips to the standard's
     * enabled modules. Other types use the requested list as-is.
     */
    public List<PermissionType> effectivePermissionTypes(List<PermissionType> requested) {
        if (this == ADMIN) {
            return Arrays.asList(PermissionType.values());
        }
        return requested == null ? List.of() : List.copyOf(requested);
    }
}
