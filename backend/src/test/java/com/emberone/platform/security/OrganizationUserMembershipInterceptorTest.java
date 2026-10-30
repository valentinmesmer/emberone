/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.emberone.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OrganizationUserMembershipInterceptorTest {

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    @Mock UserService userService;
    @InjectMocks OrganizationUserMembershipInterceptor interceptor;

    @Test
    void resolveMatchesOrganizationUserPaths() {
        assertThat(
                        OrganizationUserMembershipInterceptor.resolve(
                                "/api/v1/auth/organizations/42/users/7/certifications"))
                .isEqualTo(new OrganizationUserMembershipInterceptor.OrgUserPathIds(42L, 7L));
        assertThat(
                        OrganizationUserMembershipInterceptor.resolve(
                                "/api/v1/auth/organizations/10/users/3"))
                .isEqualTo(new OrganizationUserMembershipInterceptor.OrgUserPathIds(10L, 3L));
        assertThat(
                        OrganizationUserMembershipInterceptor.resolve(
                                "/api/v1/auth/users/organizations/5/users/9/personal-data"))
                .isEqualTo(new OrganizationUserMembershipInterceptor.OrgUserPathIds(5L, 9L));
    }

    @Test
    void resolveIgnoresNonUserScopedPaths() {
        assertThat(
                        OrganizationUserMembershipInterceptor.resolve(
                                "/api/v1/auth/organizations/42/appointments"))
                .isNull();
        assertThat(
                        OrganizationUserMembershipInterceptor.resolve(
                                "/api/v1/auth/users/organizations/42"))
                .isNull();
        assertThat(OrganizationUserMembershipInterceptor.resolve("/api/v1/self/password")).isNull();
    }

    @Test
    void preHandleAssertsMembershipForMatchedPath() {
        when(request.getRequestURI())
                .thenReturn("/api/v1/auth/organizations/10/users/7/qualifications/status");

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();

        verify(userService).assertUserBelongsToOrganization(7L, 10L);
    }

    @Test
    void preHandleAssertsMembershipForPercentEncodedPath() {
        when(request.getRequestURI())
                .thenReturn("/api/v1/auth/organizations/%31%30/users/%37/course-registrations");

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();

        verify(userService).assertUserBelongsToOrganization(7L, 10L);
    }

    @Test
    void preHandleRejectsMalformedPath() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/organizations/10/users/%zz");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(ResponseStatusException.class);

        verifyNoInteractions(userService);
    }

    @Test
    void preHandleSkipsUnmatchedPaths() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/organizations/10/appointments");

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();

        verifyNoInteractions(userService);
    }

    @Test
    void preHandlePropagatesMembershipFailure() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/organizations/10/users/7/groups");
        org.mockito.Mockito.doThrow(
                        new IllegalArgumentException(
                                "User does not belong to the specified organization"))
                .when(userService)
                .assertUserBelongsToOrganization(7L, 10L);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("organization");
    }
}
