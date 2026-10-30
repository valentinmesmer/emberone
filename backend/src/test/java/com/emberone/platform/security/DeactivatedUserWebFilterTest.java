/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class DeactivatedUserWebFilterTest {

    @Mock DeactivatedUserAccessService deactivatedUserAccessService;
    @Mock FilterChain filterChain;

    DeactivatedUserWebFilter filter;

    @BeforeEach
    void setUp() {
        filter = new DeactivatedUserWebFilter(deactivatedUserAccessService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void blocksDeactivatedUserApiCalls() throws Exception {
        Jwt jwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .claim("app_role", "USER")
                        .claim("tenantId", "7")
                        .claim("userId", "1")
                        .subject("user")
                        .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        TenantContextHolder.setTenantId(7L);
        TenantContextHolder.setUserId(1L);
        when(deactivatedUserAccessService.isActiveUser(1L)).thenReturn(false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void allowsActiveUsers() throws Exception {
        Jwt jwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .claim("app_role", "USER")
                        .claim("tenantId", "7")
                        .claim("userId", "1")
                        .subject("user")
                        .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        TenantContextHolder.setTenantId(7L);
        TenantContextHolder.setUserId(1L);
        when(deactivatedUserAccessService.isActiveUser(1L)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(deactivatedUserAccessService).isActiveUser(1L);
    }

    @Test
    void skipsWhenNoUserContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(deactivatedUserAccessService, never()).isActiveUser(anyLong());
    }

    @Test
    void skipsPlatformAdminWithoutUsersRow() throws Exception {
        Jwt jwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .claim("app_role", "GLOBAL_ADMIN")
                        .claim("userId", "0")
                        .subject("akadmin")
                        .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        TenantContextHolder.setUserId(EmberOneJwtClaims.PLATFORM_ADMIN_USER_ID);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(deactivatedUserAccessService, never()).isActiveUser(anyLong());
    }
}
