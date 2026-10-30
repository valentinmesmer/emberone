/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

class TokenClassifierTest {

    @Test
    void extractsJwtFromOAuth2ResourceServerAuthentication() {
        Jwt jwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .subject("user-1")
                        .claims(map -> map.putAll(Map.of(EmberOneJwtClaims.USER_ID, "42")))
                        .build();
        Authentication auth = new UsernamePasswordAuthenticationToken(jwt, "n/a");

        assertThat(TokenClassifier.extractJwt(auth)).isSameAs(jwt);
    }

    @Test
    void returnsNullWhenPrincipalIsNotJwt() {
        Authentication auth = new UsernamePasswordAuthenticationToken("user", "n/a");

        assertThat(TokenClassifier.extractJwt(auth)).isNull();
    }

    @Test
    void returnsNullForNullAuthentication() {
        assertThat(TokenClassifier.extractJwt(null)).isNull();
    }
}
