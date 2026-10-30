/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import java.util.List;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/** Tries multiple {@link JwtDecoder}s in order (e.g. several Authentik issuer URLs). */
public class CompositeJwtDecoder implements JwtDecoder {

    private final List<JwtDecoder> decoders;

    public CompositeJwtDecoder(List<JwtDecoder> decoders) {
        if (decoders == null || decoders.isEmpty()) {
            throw new IllegalArgumentException("At least one decoder must be provided");
        }
        this.decoders = decoders;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        JwtException lastException = null;

        for (JwtDecoder decoder : decoders) {
            try {
                return decoder.decode(token);
            } catch (JwtException e) {
                lastException = e;
            }
        }

        throw lastException != null
                ? lastException
                : new OAuth2AuthenticationException(
                        "Unable to decode JWT token with any configured decoder");
    }
}
