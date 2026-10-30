/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UriUtils;

/**
 * Admin APIs scope data by the {@code tenantId} path variable. JWT tenant claims belong to the
 * signed-in user and may differ (e.g. global admin managing another tenant). This interceptor
 * overrides {@link TenantContextHolder} from the request path <em>before</em> controller /
 * {@code @Transactional} code runs so RLS ({@code app.tenant_id}) matches the target tenant. Works
 * for global admins without {@code tenantId} / {@code userId} in the JWT: the path is the sole
 * source of tenant scope on these routes.
 */
@Component
public class AdminPathTenantContextInterceptor implements HandlerInterceptor {

    static final Pattern ADMIN_TENANT_PATH =
            Pattern.compile("^/api/v1/auth/admin/tenants/(\\d+)(?:/|$)");

    /**
     * The request path as the router sees it. Raw URIs may percent-encode path characters (for
     * example {@code %34%32} for 42), which the firewall allows but routing decodes. Null when the
     * path is null or malformed.
     */
    static String decodedRequestUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return null;
        }
        try {
            return UriUtils.decode(uri, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static Long resolveTenantId(String requestUri) {
        if (requestUri == null) {
            return null;
        }
        Matcher matcher = ADMIN_TENANT_PATH.matcher(requestUri);
        if (!matcher.find()) {
            return null;
        }
        return Long.parseLong(matcher.group(1));
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long tenantId = resolveTenantId(decodedRequestUri(request));
        if (tenantId == null) {
            return true;
        }
        TenantContextHolder.setTenantId(tenantId);
        TenantContextSupport.syncMdcFromHolder();
        return true;
    }
}
