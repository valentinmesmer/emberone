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

class EmberOneTokenChecksTest {

    @Test
    void checkUserRoleMatchesWhenRolePresent() {
        Jwt jwt = jwt(Map.of(EmberOneJwtClaims.APP_ROLE, "TENANT_ADMIN"));

        assertThat(EmberOneTokenChecks.checkUserRole(jwt, "TENANT_ADMIN")).isTrue();
        assertThat(EmberOneTokenChecks.checkUserRole(jwt, "USER")).isFalse();
    }

    @Test
    void checkUserRoleReturnsFalseForMissingInputs() {
        Jwt jwt = jwt(Map.of(EmberOneJwtClaims.APP_ROLE, "USER"));

        assertThat(EmberOneTokenChecks.checkUserRole(null, "USER")).isFalse();
        assertThat(EmberOneTokenChecks.checkUserRole(jwt, null)).isFalse();
        assertThat(EmberOneTokenChecks.checkUserRole(jwt, "  ")).isFalse();
        assertThat(EmberOneTokenChecks.checkUserRole(jwt(Map.of()), "USER")).isFalse();
    }

    private Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user")
                .claims(map -> map.putAll(claims))
                .build();
    }
}
