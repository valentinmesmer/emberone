/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects EmberOne user API calls when {@code users.deactivated_at} is set, so JWTs cannot be used
 * after deactivation even if the IdP job has not run yet.
 */
public class DeactivatedUserWebFilter extends OncePerRequestFilter {

    private final DeactivatedUserAccessService deactivatedUserAccessService;

    public DeactivatedUserWebFilter(DeactivatedUserAccessService deactivatedUserAccessService) {
        this.deactivatedUserAccessService = deactivatedUserAccessService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Jwt jwt = TenantContextSupport.currentJwt();
        Long userId = TenantContextHolder.getUserId();
        // Platform admin (akadmin, userId=0) has no users row - skip deactivation check.
        if (jwt == null
                || userId == null
                || userId == EmberOneJwtClaims.PLATFORM_ADMIN_USER_ID
                || !EmberOneUserTokenClaims.isValidForUserFacingApi(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!deactivatedUserAccessService.isActiveUser(userId)) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "User is deactivated");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
