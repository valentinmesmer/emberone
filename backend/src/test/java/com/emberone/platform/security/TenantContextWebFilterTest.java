/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TenantContextWebFilterTest {

    private final TenantContextWebFilter filter = new TenantContextWebFilter();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp() {
        Jwt jwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .claim("tenantId", "12")
                        .claim("userId", "9")
                        .subject("user")
                        .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        MDC.clear();
    }

    @Test
    void bindsHolderAndMdcForRequest() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(TenantContextHolder.getUserId()).isNull();
        assertThat(MDC.get("tenantId")).isNull();
    }

    @Test
    void bindsContextDuringFilterChain() throws Exception {
        FilterChain chain =
                (req, res) -> {
                    assertThat(TenantContextHolder.getTenantId()).isEqualTo(12L);
                    assertThat(TenantContextHolder.getUserId()).isEqualTo(9L);
                    assertThat(MDC.get("tenantId")).isEqualTo("12");
                    assertThat(MDC.get("userId")).isEqualTo("9");
                };

        filter.doFilter(request, response, chain);
    }

    @Test
    void leavesContextEmptyWithoutAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        FilterChain chain =
                (req, res) -> {
                    assertThat(TenantContextHolder.getTenantId()).isNull();
                    assertThat(TenantContextHolder.getUserId()).isNull();
                };

        filter.doFilter(request, response, chain);
    }
}
