/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.emberone.admin.tenant.cache.TenantLocaleCache;
import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.platform.cache.value.TenantLocaleCacheValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantLocaleServiceTest {

    @Mock TenantLocaleCache tenantLocaleCache;
    @InjectMocks TenantLocaleService service;

    @Test
    void nullOrUnknownTenantFallsBackToGerman() {
        assertThat(service.resolveTenantLocale(null)).isEqualTo(SupportedLocale.DE);
        verify(tenantLocaleCache, never()).loadCached(org.mockito.ArgumentMatchers.any());

        when(tenantLocaleCache.loadCached(99L))
                .thenReturn(new TenantLocaleCacheValue(SupportedLocale.DE));
        assertThat(service.resolveTenantLocale(99L)).isEqualTo(SupportedLocale.DE);
    }
}
