/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.permission.domain.PermissionGroupType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissionGroupNamingServiceTest {

    @Mock TenantLocaleService tenantLocaleService;
    @Mock LocalizationService localizationService;

    @InjectMocks PermissionGroupNamingService service;

    @Test
    void resolveLocalizedNameUsesMessagesBundle() {
        when(tenantLocaleService.resolveTenantLocale(7L)).thenReturn(SupportedLocale.DE);
        when(localizationService.getMessageOrNull("permissionGroup.ADMIN", SupportedLocale.DE))
                .thenReturn("Admin");
        when(localizationService.getMessageOrNull("permissionGroup.STANDARD", SupportedLocale.DE))
                .thenReturn("Standard");

        assertThat(service.resolveLocalizedName(PermissionGroupType.ADMIN, 7L)).isEqualTo("Admin");
        assertThat(service.resolveLocalizedName(PermissionGroupType.STANDARD, 7L))
                .isEqualTo("Standard");
    }

    @Test
    void resolveLocalizedNameFallsBackToEnumNameWhenMessageMissing() {
        when(tenantLocaleService.resolveTenantLocale(7L)).thenReturn(SupportedLocale.DE);
        when(localizationService.getMessageOrNull("permissionGroup.ADMIN", SupportedLocale.DE))
                .thenReturn(null);

        assertThat(service.resolveLocalizedName(PermissionGroupType.ADMIN, 7L)).isEqualTo("ADMIN");
    }
}
