/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import lombok.experimental.UtilityClass;
import org.springframework.security.oauth2.jwt.Jwt;

@UtilityClass
public class EmberOneUserTokenClaims {

    private static boolean hasAnyAppRole(Jwt jwt) {
        if (jwt == null) {
            return false;
        }
        return EmberOneJwtClaims.resolveAppRole(jwt)
                .map(
                        role -> {
                            for (EmberOneAppRole r : EmberOneAppRole.values()) {
                                if (r.getRoleName().equals(role)) {
                                    return true;
                                }
                            }
                            return false;
                        })
                .orElse(false);
    }

    public static boolean isGlobalAdmin(Jwt jwt) {
        if (jwt == null) {
            return false;
        }
        return EmberOneJwtClaims.resolveAppRole(jwt)
                .map(EmberOneAppRole.GLOBAL_ADMIN.getRoleName()::equals)
                .orElse(false);
    }

    public static boolean isValidForUserFacingApi(Jwt jwt) {
        return hasAnyAppRole(jwt)
                && (isGlobalAdmin(jwt) || EmberOneJwtClaims.resolveTenantId(jwt).isPresent());
    }
}
