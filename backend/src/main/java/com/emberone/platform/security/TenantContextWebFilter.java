/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Binds JWT tenant/user into {@link TenantContextHolder} for the HTTP request, before controllers
 * or {@code @Transactional} services run. Registered in {@link SecurityConfig} immediately after
 * {@link BearerTokenAuthenticationFilter}. Cleared in {@code finally} so pool threads stay clean.
 */
public class TenantContextWebFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            TenantContextSupport.bindFromJwt(TenantContextSupport.currentJwt());
            TenantContextSupport.syncMdcFromHolder();
            filterChain.doFilter(request, response);
        } finally {
            TenantContextSupport.clearMdc();
            TenantContextHolder.clear();
        }
    }
}
