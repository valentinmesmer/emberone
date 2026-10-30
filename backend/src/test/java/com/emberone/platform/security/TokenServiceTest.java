/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

class TokenServiceTest {

    private final TokenService tokenService = new TokenService();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsClaimsAndRoleFromJwtPrincipal() {
        authenticate(jwt("42", "7", EmberOneAppRole.TENANT_ADMIN.getRoleName()));

        assertThat(tokenService.getUserId()).isEqualTo(42L);
        assertThat(tokenService.requireUserId()).isEqualTo(42L);
        assertThat(tokenService.getReferencedUserId()).isEqualTo(42L);
        assertThat(tokenService.getTenantId()).isEqualTo(7L);
        assertThat(tokenService.requireTenantId()).isEqualTo(7L);
        assertThat(tokenService.getAppRole()).contains(EmberOneAppRole.TENANT_ADMIN.getRoleName());
        assertThat(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).isTrue();
        assertThat(tokenService.hasRole(EmberOneAppRole.USER.getRoleName())).isFalse();
    }

    @Test
    void returnsEmptyValuesWithoutJwtAuthentication() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("user", "password"));

        assertThat(tokenService.getUserId()).isNull();
        assertThat(tokenService.getReferencedUserId()).isNull();
        assertThat(tokenService.getTenantId()).isNull();
        assertThat(tokenService.getAppRole()).isEmpty();
        assertThat(tokenService.hasRole(EmberOneAppRole.USER.getRoleName())).isFalse();
    }

    @Test
    void referencedUserIdOmitsPlatformAdmin() {
        authenticate(
                jwt(
                        String.valueOf(EmberOneJwtClaims.PLATFORM_ADMIN_USER_ID),
                        null,
                        EmberOneAppRole.GLOBAL_ADMIN.getRoleName()));

        assertThat(tokenService.getUserId()).isEqualTo(EmberOneJwtClaims.PLATFORM_ADMIN_USER_ID);
        assertThat(tokenService.getReferencedUserId()).isNull();
    }

    @Test
    void requireUserIdNamesMissingUserClaim() {
        authenticate(jwt(null, "7", EmberOneAppRole.USER.getRoleName()));

        assertThatThrownBy(tokenService::requireUserId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No userId found in token");
    }

    @Test
    void requireTenantIdNamesMissingTenantClaim() {
        authenticate(jwt("42", null, EmberOneAppRole.USER.getRoleName()));

        assertThatThrownBy(tokenService::requireTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No tenantId found in token");
    }

    private void authenticate(Jwt jwt) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(jwt, jwt.getTokenValue()));
    }

    private Jwt jwt(String userId, String tenantId, String role) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "none");
        if (userId != null) {
            builder.claim(EmberOneJwtClaims.USER_ID, userId);
        }
        if (tenantId != null) {
            builder.claim("tenantId", tenantId);
        }
        if (role != null) {
            builder.claim("app_role", role);
        }
        return builder.build();
    }
}
