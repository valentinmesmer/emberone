/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list.options;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.platform.security.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListFilterOptionsAuthorizationTest {

    @Mock OrganizationAccessService organizationAccessService;
    @Mock TokenService tokenService;

    @InjectMocks ListFilterOptionsAuthorization authorization;

    @Test
    void tenantFilterOnlyForGlobalAdmin() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        assertThat(authorization.mayExposeTenantForAdminUserListFilter()).isFalse();

        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(true);
        assertThat(authorization.mayExposeTenantForAdminUserListFilter()).isTrue();
    }

    @Test
    void organizationFilterDelegatesToAccessService() {
        when(organizationAccessService.hasOrganizationPermission(5L, PermissionType.USER_READ))
                .thenReturn(true);
        assertThat(authorization.mayExposeOrganizationForAdminUserListFilter(5L)).isTrue();
    }
}
