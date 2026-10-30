/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class EmberOneJwtClaimsTest {

    @Test
    void resolvesUserIdFromClaim() {
        Jwt jwt = jwt(Map.of(EmberOneJwtClaims.USER_ID, "42"));

        assertThat(EmberOneJwtClaims.resolveUserId(jwt)).contains(42L);
    }

    @Test
    void resolvesUserIdFromNumericClaim() {
        Jwt jwt = jwt(Map.of(EmberOneJwtClaims.USER_ID, 42));

        assertThat(EmberOneJwtClaims.resolveUserId(jwt)).contains(42L);
    }

    @Test
    void referencedUserIdOmitsPlatformAdmin() {
        assertThat(EmberOneJwtClaims.referencedUserId(null)).isNull();
        assertThat(EmberOneJwtClaims.referencedUserId(EmberOneJwtClaims.PLATFORM_ADMIN_USER_ID))
                .isNull();
        assertThat(EmberOneJwtClaims.referencedUserId(42L)).isEqualTo(42L);
    }

    @Test
    void resolvesPlatformAdminUserIdZero() {
        assertThat(EmberOneJwtClaims.resolveUserId(jwt(Map.of(EmberOneJwtClaims.USER_ID, "0"))))
                .contains(EmberOneJwtClaims.PLATFORM_ADMIN_USER_ID);
        assertThat(EmberOneJwtClaims.resolveUserId(jwt(Map.of(EmberOneJwtClaims.USER_ID, 0))))
                .contains(EmberOneJwtClaims.PLATFORM_ADMIN_USER_ID);
    }

    @Test
    void returnsEmptyWhenUserIdClaimMissingOrInvalid() {
        assertThat(EmberOneJwtClaims.resolveUserId(jwt(Map.of()))).isEmpty();
        assertThat(
                        EmberOneJwtClaims.resolveUserId(
                                jwt(Map.of(EmberOneJwtClaims.USER_ID, "not-a-number"))))
                .isEmpty();
        assertThat(EmberOneJwtClaims.resolveUserId(null)).isEmpty();
    }

    @Test
    void resolvesTenantId() {
        Jwt jwt = jwt(Map.of(EmberOneJwtClaims.TENANT_ID, "7"));

        assertThat(EmberOneJwtClaims.resolveTenantId(jwt)).contains(7L);
    }

    @Test
    void returnsEmptyWhenTenantIdMissingOrInvalid() {
        assertThat(EmberOneJwtClaims.resolveTenantId(jwt(Map.of()))).isEmpty();
        assertThat(EmberOneJwtClaims.resolveTenantId(jwt(Map.of(EmberOneJwtClaims.TENANT_ID, "x"))))
                .isEmpty();
        assertThat(EmberOneJwtClaims.resolveTenantId(null)).isEmpty();
    }

    @Test
    void resolvesAppRoleTrimmed() {
        Jwt jwt = jwt(Map.of(EmberOneJwtClaims.APP_ROLE, "  USER  "));

        assertThat(EmberOneJwtClaims.resolveAppRole(jwt)).contains("USER");
    }

    @Test
    void returnsEmptyWhenAppRoleMissing() {
        assertThat(EmberOneJwtClaims.resolveAppRole(jwt(Map.of()))).isEmpty();
        assertThat(EmberOneJwtClaims.resolveAppRole(null)).isEmpty();
    }

    private Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user")
                .claims(map -> map.putAll(claims))
                .build();
    }
}
