/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.module.domain;

import com.emberone.permission.domain.PermissionType;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;

/**
 * Functional area of a {@link PermissionType}. {@link #CORE} is always enabled for the org; {@code
 * toggleable == true} modules can be switched off per standard (service operations).
 */
@Getter
public enum Module {
    CORE(false),
    SERVICE(true),
    OPERATIONS(true),
    APPOINTMENTS(true),
    COURSE_OFFERINGS(true),
    MANAGEMENT(true);

    private final boolean toggleable;

    Module(boolean toggleable) {
        this.toggleable = toggleable;
    }

    public static List<PermissionType> filterPermissions(
            Collection<PermissionType> permissionTypes, Set<Module> enabledModules) {
        if (permissionTypes == null || permissionTypes.isEmpty()) {
            return List.of();
        }
        return permissionTypes.stream()
                .filter(permissionType -> enabledModules.contains(permissionType.getModule()))
                .toList();
    }

    /** All modules enabled (empty {@code std_standard_modules} for a standard). */
    public static Set<Module> allEnabled() {
        return EnumSet.allOf(Module.class);
    }

    /** Base set before applying stored toggleable module rows. */
    public static Set<Module> alwaysOnModules() {
        Set<Module> modules = EnumSet.noneOf(Module.class);
        for (Module module : values()) {
            if (!module.isToggleable()) {
                modules.add(module);
            }
        }
        return modules;
    }
}
