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
import org.slf4j.MDC;

class AdminPathTenantContextInterceptorTest {

    private final AdminPathTenantContextInterceptor interceptor =
            new AdminPathTenantContextInterceptor();
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        MDC.clear();
    }

    @Test
    void resolveTenantIdFromAdminPath() {
        assertThat(
                        AdminPathTenantContextInterceptor.resolveTenantId(
                                "/api/v1/auth/admin/tenants/42/standards"))
                .isEqualTo(42L);
        assertThat(
                        AdminPathTenantContextInterceptor.resolveTenantId(
                                "/api/v1/auth/admin/tenants/7"))
                .isEqualTo(7L);
        assertThat(AdminPathTenantContextInterceptor.resolveTenantId("/api/v1/auth/admin/tenants"))
                .isNull();
        assertThat(
                        AdminPathTenantContextInterceptor.resolveTenantId(
                                "/api/v1/auth/admin/tenants/abc/standards"))
                .isNull();
    }

    @Test
    void preHandleSetsTenantContextWhenJwtHasNoTenantClaim() {
        TenantContextHolder.clear();
        when(request.getRequestURI()).thenReturn("/api/v1/auth/admin/tenants/55/standards");

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();

        assertThat(TenantContextHolder.getTenantId()).isEqualTo(55L);
        assertThat(MDC.get("tenantId")).isEqualTo("55");
    }

    @Test
    void preHandleOverridesTenantContextFromPath() {
        TenantContextHolder.setTenantId(1L);
        when(request.getRequestURI())
                .thenReturn("/api/v1/auth/admin/tenants/99/standards/assignments");

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();

        assertThat(TenantContextHolder.getTenantId()).isEqualTo(99L);
        assertThat(MDC.get("tenantId")).isEqualTo("99");
    }

    @Test
    void preHandleIgnoresNonAdminTenantPaths() {
        TenantContextHolder.setTenantId(1L);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/admin/tenants");

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();

        assertThat(TenantContextHolder.getTenantId()).isEqualTo(1L);
        assertThat(MDC.get("tenantId")).isNull();
    }

    @Test
    void preHandleDecodesPercentEncodedTenantId() {
        TenantContextHolder.clear();
        when(request.getRequestURI())
                .thenReturn("/api/v1/auth/admin/tenants/%34%32/organizations/tree");

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();

        assertThat(TenantContextHolder.getTenantId()).isEqualTo(42L);
    }
}
