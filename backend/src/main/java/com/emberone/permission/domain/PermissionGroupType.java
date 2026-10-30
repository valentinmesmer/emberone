/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
     * ADMIN receives every {@link PermissionType} except {@link PermissionType#MANAGEMENT_ACCESS},
     * which must be granted explicitly. Other types use the requested list as-is.
     */
    public List<PermissionType> effectivePermissionTypes(List<PermissionType> requested) {
        if (this == ADMIN) {
            return Arrays.stream(PermissionType.values())
                    .filter(type -> type != PermissionType.MANAGEMENT_ACCESS)
                    .toList();
        }
        return requested == null ? List.of() : List.copyOf(requested);
    }
}
