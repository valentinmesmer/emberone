/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.tenant.cache;

import com.emberone.platform.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class TenantLocaleCacheEvictor {

    @CacheEvict(cacheNames = CacheNames.TENANT_LOCALE, key = "#tenantId")
    public void evict(Long tenantId) {}
}
