/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import lombok.experimental.UtilityClass;
import org.springframework.security.oauth2.jwt.Jwt;

@UtilityClass
public class EmberOneTokenChecks {

    public static boolean checkUserRole(Jwt token, String requiredRole) {
        if (token == null || requiredRole == null || requiredRole.isBlank()) {
            return false;
        }
        return EmberOneJwtClaims.resolveAppRole(token).map(requiredRole::equals).orElse(false);
    }
}
