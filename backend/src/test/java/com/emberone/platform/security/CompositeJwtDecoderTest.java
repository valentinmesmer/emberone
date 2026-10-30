/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class CompositeJwtDecoderTest {

    @Test
    void decodeReturnsFirstSuccessfulDecoderResult() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("user").build();
        JwtDecoder first = mock(JwtDecoder.class);
        JwtDecoder second = mock(JwtDecoder.class);
        when(first.decode("token")).thenThrow(new JwtException("first failed"));
        when(second.decode("token")).thenReturn(jwt);

        CompositeJwtDecoder decoder = new CompositeJwtDecoder(List.of(first, second));

        assertThat(decoder.decode("token")).isSameAs(jwt);
    }

    @Test
    void decodeThrowsLastExceptionWhenAllDecodersFail() {
        JwtDecoder first = mock(JwtDecoder.class);
        JwtDecoder second = mock(JwtDecoder.class);
        JwtException secondError = new JwtException("second failed");
        when(first.decode("token")).thenThrow(new JwtException("first failed"));
        when(second.decode("token")).thenThrow(secondError);

        CompositeJwtDecoder decoder = new CompositeJwtDecoder(List.of(first, second));

        assertThatThrownBy(() -> decoder.decode("token")).isSameAs(secondError);
    }

    @Test
    void constructorRejectsEmptyDecoderList() {
        assertThatThrownBy(() -> new CompositeJwtDecoder(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one decoder");
    }
}
