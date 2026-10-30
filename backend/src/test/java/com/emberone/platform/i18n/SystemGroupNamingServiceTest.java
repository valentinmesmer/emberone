/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemGroupNamingServiceTest {

    @Mock TenantLocaleService tenantLocaleService;
    @Mock LocalizationService localizationService;

    @InjectMocks SystemGroupNamingService service;

    @Test
    void resolveLocalizedNameUsesMessagesBundle() {
        when(tenantLocaleService.resolveTenantLocale(7L)).thenReturn(SupportedLocale.DE);
        when(localizationService.getMessageOrNull(
                        "organizationGroup.ALL_MEMBERS", SupportedLocale.DE))
                .thenReturn("Alle Mitglieder");
        when(localizationService.getMessageOrNull(
                        "organizationGroup.LEADERSHIP", SupportedLocale.DE))
                .thenReturn("Führung");

        assertThat(service.resolveLocalizedName(OrganizationGroupType.ALL_MEMBERS, 7L))
                .isEqualTo("Alle Mitglieder");
        assertThat(service.resolveLocalizedName(OrganizationGroupType.LEADERSHIP, 7L))
                .isEqualTo("Führung");
    }

    @Test
    void resolveLocalizedNameFallsBackToEnumNameWhenMessageMissing() {
        when(tenantLocaleService.resolveTenantLocale(7L)).thenReturn(SupportedLocale.DE);
        when(localizationService.getMessageOrNull(
                        "organizationGroup.LEADERSHIP", SupportedLocale.DE))
                .thenReturn(null);

        assertThat(service.resolveLocalizedName(OrganizationGroupType.LEADERSHIP, 7L))
                .isEqualTo("LEADERSHIP");
    }
}
