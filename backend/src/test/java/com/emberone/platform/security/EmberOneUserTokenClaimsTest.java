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

class EmberOneUserTokenClaimsTest {

    @Test
    void isGlobalAdminOnlyForGlobalAdminRole() {
        assertThat(
                        EmberOneUserTokenClaims.isGlobalAdmin(
                                jwt(Map.of(EmberOneJwtClaims.APP_ROLE, "GLOBAL_ADMIN"))))
                .isTrue();
        assertThat(
                        EmberOneUserTokenClaims.isGlobalAdmin(
                                jwt(Map.of(EmberOneJwtClaims.APP_ROLE, "USER"))))
                .isFalse();
    }

    @Test
    void isValidForUserFacingApiRequiresKnownRoleAndTenantOrGlobalAdmin() {
        assertThat(
                        EmberOneUserTokenClaims.isValidForUserFacingApi(
                                jwt(
                                        Map.of(
                                                EmberOneJwtClaims.APP_ROLE, "USER",
                                                EmberOneJwtClaims.TENANT_ID, "5"))))
                .isTrue();

        assertThat(
                        EmberOneUserTokenClaims.isValidForUserFacingApi(
                                jwt(Map.of(EmberOneJwtClaims.APP_ROLE, "GLOBAL_ADMIN"))))
                .isTrue();

        assertThat(
                        EmberOneUserTokenClaims.isValidForUserFacingApi(
                                jwt(Map.of(EmberOneJwtClaims.APP_ROLE, "USER"))))
                .isFalse();

        assertThat(
                        EmberOneUserTokenClaims.isValidForUserFacingApi(
                                jwt(Map.of(EmberOneJwtClaims.APP_ROLE, "UNKNOWN"))))
                .isFalse();

        assertThat(EmberOneUserTokenClaims.isValidForUserFacingApi(null)).isFalse();
    }

    private Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user")
                .claims(map -> map.putAll(claims))
                .build();
    }
}
