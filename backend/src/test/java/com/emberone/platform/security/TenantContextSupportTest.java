/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.platform.realtime.auth.EmberOnePrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TenantContextSupportTest {

    private static Jwt jwt(String tenantId, String userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("tenantId", tenantId)
                .claim("userId", userId)
                .subject("user")
                .expiresAt(java.time.Instant.now().plusSeconds(3600))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        MDC.clear();
    }

    @Test
    void bindFromJwtSetsHolder() {
        Jwt jwt = jwt("12", "9");

        TenantContextSupport.bindFromJwt(jwt);

        assertThat(TenantContextHolder.getTenantId()).isEqualTo(12L);
        assertThat(TenantContextHolder.getUserId()).isEqualTo(9L);
    }

    @Test
    void resolvePrefersHolderOverJwt() {
        Jwt jwt = jwt("1", "2");
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        TenantContextHolder.setTenantId(99L);
        TenantContextHolder.setUserId(100L);

        assertThat(TenantContextSupport.resolveTenantId()).isEqualTo(99L);
        assertThat(TenantContextSupport.resolveUserId()).isEqualTo(100L);
    }

    @Test
    void resolveFallsBackToJwtWhenHolderEmpty() {
        Jwt jwt = jwt("5", "6");
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertThat(TenantContextSupport.resolveTenantId()).isEqualTo(5L);
        assertThat(TenantContextSupport.resolveUserId()).isEqualTo(6L);
    }

    @Test
    void bindFromPrincipalSetsTenantAndUser() {
        Jwt jwt = jwt("3", "4");
        EmberOnePrincipal principal = new EmberOnePrincipal(3L, 4L, jwt);

        TenantContextSupport.bindFromPrincipal(principal);

        assertThat(TenantContextHolder.getTenantId()).isEqualTo(3L);
        assertThat(TenantContextHolder.getUserId()).isEqualTo(4L);
        assertThat(MDC.get("tenantId")).isEqualTo("3");
        assertThat(MDC.get("userId")).isEqualTo("4");
    }

    @Test
    void syncMdcFromHolder() {
        TenantContextHolder.setTenantId(7L);
        TenantContextHolder.setUserId(8L);

        TenantContextSupport.syncMdcFromHolder();

        assertThat(MDC.get("tenantId")).isEqualTo("7");
        assertThat(MDC.get("userId")).isEqualTo("8");
    }
}
