/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userpermission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.userpermission.cache.UserPermissionCache;
import com.emberone.userpermission.dto.UserPermissionDto;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserPermissionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long TENANT_ID = 7L;
    private static final Long ORG_ID = 10L;

    @Mock private UserPermissionCache cachedUserPermissionLoader;
    @Mock private TenantLocaleService tenantLocaleService;

    @InjectMocks private UserPermissionService service;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void getUserPermissions_returnsPermissionsAndResolvesLocaleSeparately() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(cachedUserPermissionLoader.load(TENANT_ID, USER_ID))
                .thenReturn(
                        UserPermissionDto.builder()
                                .permissions(
                                        Map.of(
                                                ORG_ID,
                                                List.of(PermissionType.USER_READ),
                                                11L,
                                                List.of(PermissionType.USER_WRITE)))
                                .build());
        when(tenantLocaleService.resolveTenantLocale(TENANT_ID)).thenReturn(SupportedLocale.DE);

        var result = service.getUserPermissions(USER_ID);

        assertThat(result.getPermissions()).containsOnlyKeys(ORG_ID, 11L);
        assertThat(result.getPermissions().get(ORG_ID)).containsExactly(PermissionType.USER_READ);
        assertThat(result.getTenantLocale()).isEqualTo(SupportedLocale.DE);
        verify(tenantLocaleService).resolveTenantLocale(TENANT_ID);
    }

    @Test
    void getUserPermissions_unknownOrForeignUserResolvesToEmptyMap() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(cachedUserPermissionLoader.load(TENANT_ID, USER_ID))
                .thenReturn(UserPermissionDto.builder().permissions(Map.of()).build());
        when(tenantLocaleService.resolveTenantLocale(TENANT_ID)).thenReturn(SupportedLocale.DE);

        assertThat(service.getUserPermissions(USER_ID).getPermissions()).isEmpty();
    }

    @Test
    void getUserPermissions_requiresActiveTenant() {
        assertThatThrownBy(() -> service.getUserPermissions(USER_ID))
                .isInstanceOf(IllegalStateException.class);
    }
}
