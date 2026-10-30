/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.user.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.admin.list.options.AdminOrganizationIdFilterOptionsProvider;
import com.emberone.admin.list.options.AdminTenantIdFilterOptionsProvider;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.options.ListFilterOptionDto;
import com.emberone.platform.web.list.options.ListFilterOptionsContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUserListFilterOptionsTest {

    @Mock TokenService tokenService;
    @Mock AdminTenantIdFilterOptionsProvider tenantIdOptions;
    @Mock AdminOrganizationIdFilterOptionsProvider organizationIdOptions;

    @InjectMocks AdminUserListFilterOptions adminUserListFilterOptions;

    @Test
    void tenantAdminDoesNotExposeTenantIdKey() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(organizationIdOptions.resolve(ListFilterOptionsContext.admin())).thenReturn(List.of());

        var dto = adminUserListFilterOptions.filterOptions();

        assertThat(dto.options()).doesNotContainKey("tenantId");
        assertThat(dto.options()).containsOnlyKeys("organizationId");
    }

    @Test
    void globalAdminIncludesTenantId() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(true);
        when(tenantIdOptions.resolve(ListFilterOptionsContext.admin()))
                .thenReturn(List.of(new ListFilterOptionDto("1", "T1")));
        when(organizationIdOptions.resolve(ListFilterOptionsContext.admin())).thenReturn(List.of());

        var dto = adminUserListFilterOptions.filterOptions();

        assertThat(dto.options()).containsKeys("tenantId", "organizationId");
        assertThat(dto.options()).doesNotContainKey("appRole");
    }
}
