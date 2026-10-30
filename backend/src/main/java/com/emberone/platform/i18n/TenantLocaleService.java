/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.i18n;

import com.emberone.admin.tenant.cache.TenantLocaleCache;
import com.emberone.admin.tenant.domain.SupportedLocale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantLocaleService {

    private final TenantLocaleCache tenantLocaleCache;

    @Transactional(readOnly = true)
    public SupportedLocale resolveTenantLocale(Long tenantId) {
        if (tenantId == null) {
            return SupportedLocale.DE;
        }
        return tenantLocaleCache.loadCached(tenantId).locale();
    }
}
