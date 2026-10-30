/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import com.emberone.platform.realtime.auth.EmberOnePrincipal;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Single place to bind {@link TenantContextHolder} and to resolve tenant/user for DB session GUCs
 * ({@code app.tenant_id}, {@code app.user_id}).
 *
 * <ul>
 *   <li>HTTP: {@link TenantContextWebFilter} (after bearer auth in {@link SecurityConfig}) binds
 *       from JWT; admin routes may override tenant via {@link AdminPathTenantContextInterceptor}.
 *   <li>DB: {@link TenantContextJpaTransactionManager} applies {@link PostgresSessionContext} at
 *       transaction begin.
 *   <li>WebSocket: bind via {@link #bindFromPrincipal} where STOMP handlers run.
 * </ul>
 *
 * Holder values take precedence over JWT claims.
 */
@UtilityClass
public class TenantContextSupport {

    @Nullable public static Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }

    /**
     * Populate the holder from JWT claims when present. Does not clear values missing from the
     * token.
     */
    public static void bindFromJwt(@Nullable Jwt jwt) {
        if (jwt == null) {
            return;
        }
        EmberOneJwtClaims.resolveTenantId(jwt).ifPresent(TenantContextHolder::setTenantId);
        EmberOneJwtClaims.resolveUserId(jwt).ifPresent(TenantContextHolder::setUserId);
    }

    public static void bindFromPrincipal(EmberOnePrincipal principal) {
        TenantContextHolder.setTenantId(principal.getTenantId());
        TenantContextHolder.setUserId(principal.getUserId());
        syncMdcFromHolder();
    }

    /** Effective tenant for RLS / audit: {@link TenantContextHolder} first, then JWT. */
    @Nullable public static Long resolveTenantId() {
        return resolveTenantId(currentJwt());
    }

    @Nullable public static Long resolveTenantId(@Nullable Jwt jwt) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        if (jwt == null) {
            return null;
        }
        return EmberOneJwtClaims.resolveTenantId(jwt).orElse(null);
    }

    /** Effective user for audit: {@link TenantContextHolder} first, then JWT. */
    @Nullable public static Long resolveUserId() {
        return resolveUserId(currentJwt());
    }

    @Nullable public static Long resolveUserId(@Nullable Jwt jwt) {
        Long userId = TenantContextHolder.getUserId();
        if (userId != null) {
            return userId;
        }
        if (jwt == null) {
            return null;
        }
        return EmberOneJwtClaims.resolveUserId(jwt).orElse(null);
    }

    public static void syncMdcFromHolder() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            MDC.put("tenantId", String.valueOf(tenantId));
        }
        Long userId = TenantContextHolder.getUserId();
        if (userId != null) {
            MDC.put("userId", String.valueOf(userId));
        }
    }

    public static void clearMdc() {
        MDC.remove("tenantId");
        MDC.remove("userId");
    }
}
