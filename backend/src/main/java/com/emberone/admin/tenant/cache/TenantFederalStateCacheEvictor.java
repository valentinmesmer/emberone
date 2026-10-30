/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.cache;

import com.emberone.platform.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
public class TenantFederalStateCacheEvictor {

    @CacheEvict(cacheNames = CacheNames.TENANT_FEDERAL_STATE, key = "#tenantId")
    public void evict(Long tenantId) {}
}
