/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
