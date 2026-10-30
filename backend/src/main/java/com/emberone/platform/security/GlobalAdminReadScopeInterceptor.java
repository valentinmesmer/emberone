/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

/**
 * Opens cross-tenant <em>reads</em> for global admins, and only there: GET/HEAD on admin routes
 * that are not bound to one tenant by path ({@code /api/v1/auth/admin/tenants/{id}/...} keeps its
 * single-tenant scope, see {@link AdminPathTenantContextInterceptor}). Tenant admins, users and
 * every non-admin route never get the flag; writes always stay tenant-scoped. The flag becomes the
 * GUC {@code app.global_admin}, which only dedicated SELECT policies honor.
 */
@Component
@RequiredArgsConstructor
public class GlobalAdminReadScopeInterceptor implements AsyncHandlerInterceptor {

    static final String ADMIN_PREFIX = "/api/v1/auth/admin/";

    private final TokenService tokenService;

    static boolean isCrossTenantAdminRead(String method, String requestUri) {
        boolean read = HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method);
        return read
                && requestUri != null
                && requestUri.startsWith(ADMIN_PREFIX)
                && AdminPathTenantContextInterceptor.resolveTenantId(requestUri) == null;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isCrossTenantAdminRead(
                        request.getMethod(),
                        AdminPathTenantContextInterceptor.decodedRequestUri(request))
                && tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())) {
            TenantContextHolder.setGlobalAdminRead(true);
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        TenantContextHolder.setGlobalAdminRead(false);
    }

    @Override
    public void afterConcurrentHandlingStarted(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        TenantContextHolder.setGlobalAdminRead(false);
    }
}
