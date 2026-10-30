/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void echoesAcceptedRequestIdAndSetsMdcDuringChain() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        whenHeader(request, RequestIdSupport.HEADER_NAME, "client-req-99");

        FilterChain chain =
                (req, res) ->
                        assertThat(MDC.get(RequestIdSupport.MDC_KEY)).isEqualTo("client-req-99");

        filter.doFilter(request, response, chain);

        verify(response).setHeader(RequestIdSupport.HEADER_NAME, "client-req-99");
        assertThat(MDC.get(RequestIdSupport.MDC_KEY)).isNull();
    }

    @Test
    void generatesRequestIdWhenHeaderInvalid() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        whenHeader(request, RequestIdSupport.HEADER_NAME, "not valid");

        String[] duringChain = new String[1];
        FilterChain chain = (req, res) -> duringChain[0] = MDC.get(RequestIdSupport.MDC_KEY);

        filter.doFilter(request, response, chain);

        assertThat(duringChain[0])
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        verify(response).setHeader(RequestIdSupport.HEADER_NAME, duringChain[0]);
        assertThat(MDC.get(RequestIdSupport.MDC_KEY)).isNull();
    }

    @Test
    void clearsMdcWhenChainFails() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        whenHeader(request, RequestIdSupport.HEADER_NAME, null);

        FilterChain chain =
                (req, res) -> {
                    throw new RuntimeException("boom");
                };

        try {
            filter.doFilter(request, response, chain);
        } catch (RuntimeException ignored) {
            // expected
        }

        assertThat(MDC.get(RequestIdSupport.MDC_KEY)).isNull();
    }

    private static void whenHeader(HttpServletRequest request, String name, String value) {
        org.mockito.Mockito.when(request.getHeader(name)).thenReturn(value);
    }
}
