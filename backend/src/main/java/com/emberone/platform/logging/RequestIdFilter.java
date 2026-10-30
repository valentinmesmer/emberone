/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Accepts or generates {@link RequestIdSupport#HEADER_NAME}, echoes it on the response, and binds
 * {@link RequestIdSupport#MDC_KEY} for the request. Cleared in {@code finally} independently of
 * tenant MDC ({@link com.emberone.platform.security.TenantContextSupport#clearMdc()}).
 */
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId =
                RequestIdSupport.resolve(request.getHeader(RequestIdSupport.HEADER_NAME));
        RequestIdSupport.putMdc(requestId);
        response.setHeader(RequestIdSupport.HEADER_NAME, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            RequestIdSupport.clearMdc();
        }
    }
}
