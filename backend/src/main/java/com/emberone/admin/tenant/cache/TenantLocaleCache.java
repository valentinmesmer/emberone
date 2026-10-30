/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.cache;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.admin.tenant.repository.TenantRepository;
import com.emberone.platform.cache.CacheNames;
import com.emberone.platform.cache.value.TenantLocaleCacheValue;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantLocaleCache {

    private final TenantRepository tenantRepository;

    @Cacheable(cacheNames = CacheNames.TENANT_LOCALE, key = "#tenantId")
    public TenantLocaleCacheValue loadCached(Long tenantId) {
        SupportedLocale locale =
                tenantRepository
                        .findById(tenantId)
                        .map(Tenant::getLocale)
                        .orElse(SupportedLocale.DE);
        return new TenantLocaleCacheValue(locale);
    }
}
