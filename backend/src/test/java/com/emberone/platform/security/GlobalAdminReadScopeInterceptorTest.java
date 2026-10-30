/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GlobalAdminReadScopeInterceptorTest {

    private final TokenService tokenService = mock(TokenService.class);
    private final GlobalAdminReadScopeInterceptor interceptor =
            new GlobalAdminReadScopeInterceptor(tokenService);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private boolean flagFor(String method, String uri, boolean globalAdmin) {
        TenantContextHolder.clear();
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(uri);
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName()))
                .thenReturn(globalAdmin);
        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        return TenantContextHolder.isGlobalAdminRead();
    }

    @Test
    void globalAdminReadsAcrossTenantsOnCrossTenantAdminRoutes() {
        assertThat(flagFor("GET", "/api/v1/auth/admin/users", true)).isTrue();
        assertThat(flagFor("GET", "/api/v1/auth/admin/management-activations", true)).isTrue();
        assertThat(flagFor("HEAD", "/api/v1/auth/admin/users", true)).isTrue();
    }

    @Test
    void tenantAdminAndUsersNeverGetTheFlag() {
        assertThat(flagFor("GET", "/api/v1/auth/admin/users", false)).isFalse();
        assertThat(flagFor("GET", "/api/v1/auth/admin/management-activations", false)).isFalse();
    }

    @Test
    void routesOutsideTheAdminAreaNeverGetTheFlag() {
        assertThat(flagFor("GET", "/api/v1/auth/organizations/5/users", true)).isFalse();
        assertThat(flagFor("GET", "/api/v1/self/organizations", true)).isFalse();
        assertThat(flagFor("GET", "/api/v1/auth/administrators", true)).isFalse();
    }

    @Test
    void tenantScopedAdminRoutesKeepSingleTenantScope() {
        assertThat(flagFor("GET", "/api/v1/auth/admin/tenants/42/organizations", true)).isFalse();
        assertThat(flagFor("GET", "/api/v1/auth/admin/tenants/42", true)).isFalse();
    }

    @Test
    void percentEncodedTenantIdStillCountsAsTenantScopedRoute() {
        assertThat(flagFor("GET", "/api/v1/auth/admin/tenants/%34%32/organizations/tree", true))
                .isFalse();
        assertThat(flagFor("GET", "/api/v1/auth/admin/%75sers", true)).isTrue();
    }

    @Test
    void malformedEncodingFailsClosed() {
        assertThat(flagFor("GET", "/api/v1/auth/admin/users%zz", true)).isFalse();
    }

    @Test
    void flagIsClearedAfterCompletionAndWhenAsyncHandlingStarts() {
        assertThat(flagFor("GET", "/api/v1/auth/admin/users", true)).isTrue();
        interceptor.afterCompletion(request, response, new Object(), null);
        assertThat(TenantContextHolder.isGlobalAdminRead()).isFalse();

        assertThat(flagFor("GET", "/api/v1/auth/admin/users", true)).isTrue();
        interceptor.afterConcurrentHandlingStarted(request, response, new Object());
        assertThat(TenantContextHolder.isGlobalAdminRead()).isFalse();
    }

    @Test
    void writesNeverGetTheFlag() {
        assertThat(flagFor("POST", "/api/v1/auth/admin/users", true)).isFalse();
        assertThat(flagFor("PATCH", "/api/v1/auth/admin/management-activations", true)).isFalse();
        assertThat(flagFor("DELETE", "/api/v1/auth/admin/users/1", true)).isFalse();
    }
}
