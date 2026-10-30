/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmberOneAppRole {
    GLOBAL_ADMIN("GLOBAL_ADMIN", 4),
    TENANT_ADMIN("TENANT_ADMIN", 3),
    USER("USER", 1);

    private final String roleName;
    private final int rank;

    public static Optional<EmberOneAppRole> fromRoleName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String normalized = name.trim().toUpperCase();
        for (EmberOneAppRole r : values()) {
            if (r.roleName.equals(normalized)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    public static boolean actorMayAssignRole(EmberOneAppRole actor, EmberOneAppRole assignedRole) {
        if (actor == null || assignedRole == null) {
            return false;
        }
        return assignedRole.rank <= actor.rank;
    }
}
