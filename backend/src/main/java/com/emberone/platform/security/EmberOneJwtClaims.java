/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.springframework.security.oauth2.jwt.Jwt;

@UtilityClass
public class EmberOneJwtClaims {

    /**
     * Global EmberOne user id ({@code users.id} for tenant users). JWT claim {@value #USER_ID}.
     * {@link #PLATFORM_ADMIN_USER_ID} is reserved for akadmin (no {@code users} row).
     */
    public static final String USER_ID = "userId";

    /**
     * Platform operator (akadmin); appears in {@code action_log.user_id}. {@code null} user_id
     * means system/background.
     */
    public static final long PLATFORM_ADMIN_USER_ID = 0L;

    /**
     * Id safe to store in a nullable foreign key to {@code users}. Platform admin has no {@code
     * users} row, so the column stays {@code null}. The actor is still recorded as {@code 0} in
     * {@code action_log.user_id} via {@code app.user_id}.
     */
    public static Long referencedUserId(Long userId) {
        if (userId == null || userId == PLATFORM_ADMIN_USER_ID) {
            return null;
        }
        return userId;
    }

    public static final String TENANT_ID = "tenantId";
    public static final String APP_ROLE = "app_role";

    public static Optional<Long> resolveUserId(Jwt jwt) {
        return parseLongClaim(jwt, USER_ID);
    }

    public static Optional<Long> resolveTenantId(Jwt jwt) {
        return parseLongClaim(jwt, TENANT_ID);
    }

    private static Optional<Long> parseLongClaim(Jwt jwt, String claimName) {
        if (jwt == null) {
            return Optional.empty();
        }
        Object raw = jwt.getClaims().get(claimName);
        if (raw == null) {
            return Optional.empty();
        }
        if (raw instanceof Number number) {
            return Optional.of(number.longValue());
        }
        String text = raw.toString();
        if (text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(text.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static Optional<String> resolveAppRole(Jwt jwt) {
        if (jwt == null) {
            return Optional.empty();
        }
        String role = jwt.getClaimAsString(APP_ROLE);
        if (role == null || role.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(role.trim());
    }
}
