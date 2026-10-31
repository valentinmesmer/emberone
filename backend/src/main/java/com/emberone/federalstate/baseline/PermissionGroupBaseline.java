/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.federalstate.baseline;

import com.emberone.permission.domain.PermissionGroupType;
import com.emberone.permission.domain.PermissionType;
import java.util.List;

public record PermissionGroupBaseline(
        String configKey,
        String name,
        String description,
        PermissionGroupType groupType,
        List<PermissionType> permissionTypes) {

    public PermissionGroupBaseline {
        permissionTypes = permissionTypes == null ? List.of() : List.copyOf(permissionTypes);
    }
}
