/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.exceptionhandler;

import com.emberone.platform.security.EmberOneJwtClaims;
import com.emberone.platform.security.TokenClassifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@UtilityClass
public class AccessDeniedLogSupport {

    private static final String UNKNOWN = "unknown";

    public static void logAccessDenied(Logger log, String reason) {
        log.warn(
                "Access denied: {} [{} {}] userId={} tenantId={} appRole={}",
                reason,
                requestMethod(),
                requestPath(),
                resolveUserId(),
                resolveTenantId(),
                resolveAppRole());
    }

    private static String requestMethod() {
        HttpServletRequest request = currentRequest();
        return request != null ? request.getMethod() : UNKNOWN;
    }

    private static String requestPath() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return UNKNOWN;
        }
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            return request.getRequestURI() + "?" + query;
        }
        return request.getRequestURI();
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private static Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return TokenClassifier.extractJwt(authentication);
    }

    private static String resolveUserId() {
        Jwt jwt = currentJwt();
        if (jwt == null) {
            return UNKNOWN;
        }
        String userId = EmberOneJwtClaims.resolveUserId(jwt).map(String::valueOf).orElse(null);
        return userId != null ? userId : UNKNOWN;
    }

    private static String resolveTenantId() {
        Jwt jwt = currentJwt();
        if (jwt == null) {
            return UNKNOWN;
        }
        return EmberOneJwtClaims.resolveTenantId(jwt).map(String::valueOf).orElse(UNKNOWN);
    }

    private static String resolveAppRole() {
        Jwt jwt = currentJwt();
        if (jwt == null) {
            return UNKNOWN;
        }
        return EmberOneJwtClaims.resolveAppRole(jwt).orElse(UNKNOWN);
    }
}
