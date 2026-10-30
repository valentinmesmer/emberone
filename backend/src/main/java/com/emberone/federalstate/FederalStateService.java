/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate;

import com.emberone.admin.tenant.cache.TenantFederalStateCache;
import com.emberone.federalstate.domain.FederalState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FederalStateService {

    private final TenantFederalStateCache tenantFederalStateCache;
    private final FederalStateSettingsRegistry federalStateSettingsRegistry;

    @Transactional(readOnly = true)
    public FederalState resolveTenantFederalState(Long tenantId) {
        if (tenantId == null) {
            return FederalState.BAYERN;
        }
        return tenantFederalStateCache.loadCached(tenantId).federalState();
    }

    @Transactional(readOnly = true)
    public FederalStateSettings settings(Long tenantId) {
        return settings(resolveTenantFederalState(tenantId));
    }

    @Transactional(readOnly = true)
    public FederalStateSettings settings(FederalState federalState) {
        return federalStateSettingsRegistry.require(federalState);
    }
}
