/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TokenService {

    public Long getUserId() {
        Jwt jwt = TenantContextSupport.currentJwt();
        if (jwt == null) {
            return null;
        }
        return EmberOneJwtClaims.resolveUserId(jwt).orElse(null);
    }

    public Long requireUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new IllegalStateException("No userId found in token");
        }
        return userId;
    }

    /**
     * Caller id for a nullable {@code users} foreign key. See {@link
     * EmberOneJwtClaims#referencedUserId(Long)}.
     */
    public Long getReferencedUserId() {
        return EmberOneJwtClaims.referencedUserId(getUserId());
    }

    public Long getTenantId() {
        Jwt jwt = TenantContextSupport.currentJwt();
        if (jwt == null) {
            return null;
        }
        return EmberOneJwtClaims.resolveTenantId(jwt).orElse(null);
    }

    public Long requireTenantId() {
        Long tenantId = getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenantId found in token");
        }
        return tenantId;
    }

    public boolean hasRole(String requiredRole) {
        Jwt jwt = TenantContextSupport.currentJwt();
        if (jwt == null) {
            return false;
        }
        return EmberOneTokenChecks.checkUserRole(jwt, requiredRole);
    }

    public Optional<String> getAppRole() {
        Jwt jwt = TenantContextSupport.currentJwt();
        if (jwt == null) {
            return Optional.empty();
        }
        return EmberOneJwtClaims.resolveAppRole(jwt);
    }
}
