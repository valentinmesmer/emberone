/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAccessServiceTest {

    @Mock private TokenService tokenService;

    @InjectMocks private AdminAccessService service;

    @Test
    void globalAdminCanAccessEveryTenant() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(true);

        assertThat(service.canAccessTenant(99L)).isTrue();
        assertThat(service.canListTenants()).isTrue();
        assertThat(service.isAdminUser()).isTrue();
    }

    @Test
    void tenantAdminCanOnlyAccessJwtTenant() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(true);
        when(tokenService.getTenantId()).thenReturn(7L);

        assertThat(service.canAccessTenant(7L)).isTrue();
        assertThat(service.canAccessTenant(8L)).isFalse();
    }

    @Test
    void tenantAdminWithoutTenantClaimIsDenied() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(true);
        when(tokenService.getTenantId()).thenReturn(null);

        assertThat(service.canAccessTenant(7L)).isFalse();
    }

    @Test
    void regularUserHasNoAdminAccess() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(false);

        assertThat(service.canAccessTenant(7L)).isFalse();
        assertThat(service.canListTenants()).isFalse();
        assertThat(service.isAdminUser()).isFalse();
    }

    @Test
    void isSelfServiceUser_requiresTenantUserId() {
        when(tokenService.getUserId()).thenReturn(null);
        assertThat(service.isSelfServiceUser()).isFalse();

        when(tokenService.getUserId()).thenReturn(EmberOneJwtClaims.PLATFORM_ADMIN_USER_ID);
        assertThat(service.isSelfServiceUser()).isFalse();

        when(tokenService.getUserId()).thenReturn(5L);
        assertThat(service.isSelfServiceUser()).isTrue();
    }
}
