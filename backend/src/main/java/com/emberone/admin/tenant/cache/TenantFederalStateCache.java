/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.cache;

import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.admin.tenant.repository.TenantRepository;
import com.emberone.federalstate.domain.FederalState;
import com.emberone.platform.cache.CacheNames;
import com.emberone.platform.cache.value.TenantFederalStateCacheValue;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantFederalStateCache {

    private final TenantRepository tenantRepository;

    @Cacheable(cacheNames = CacheNames.TENANT_FEDERAL_STATE, key = "#tenantId")
    public TenantFederalStateCacheValue loadCached(Long tenantId) {
        FederalState federalState =
                tenantRepository
                        .findById(tenantId)
                        .map(Tenant::getFederalState)
                        .orElse(FederalState.BAYERN);
        return new TenantFederalStateCacheValue(federalState);
    }
}
