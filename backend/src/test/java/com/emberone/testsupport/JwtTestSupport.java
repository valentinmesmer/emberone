/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.testsupport;

import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.EmberOneJwtClaims;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class JwtTestSupport {

    private JwtTestSupport() {}

    public static RequestPostProcessor jwtForRole(
            EmberOneAppRole role, Long tenantId, Long userId) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put(EmberOneJwtClaims.APP_ROLE, role.getRoleName());
        if (tenantId != null) {
            claims.put(EmberOneJwtClaims.TENANT_ID, tenantId.toString());
        }
        if (userId != null) {
            claims.put(EmberOneJwtClaims.USER_ID, userId.toString());
        }
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(
                        builder ->
                                builder.tokenValue("test-token")
                                        .header("alg", "none")
                                        .subject("user")
                                        .claims(
                                                existing -> {
                                                    existing.clear();
                                                    existing.putAll(claims);
                                                }));
    }

    public static RequestPostProcessor jwtWithoutAppRole(Long tenantId) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(
                        builder ->
                                builder.tokenValue("test-token")
                                        .header("alg", "none")
                                        .subject("user")
                                        .claim(EmberOneJwtClaims.TENANT_ID, tenantId.toString()));
    }

    public static Jwt buildJwt(EmberOneAppRole role, Long tenantId, Long userId) {
        Jwt.Builder builder =
                Jwt.withTokenValue("test-token")
                        .header("alg", "none")
                        .claim(EmberOneJwtClaims.APP_ROLE, role.getRoleName())
                        .subject("user");
        if (tenantId != null) {
            builder.claim(EmberOneJwtClaims.TENANT_ID, tenantId.toString());
        }
        if (userId != null) {
            builder.claim(EmberOneJwtClaims.USER_ID, userId.toString());
        }
        return builder.build();
    }
}
