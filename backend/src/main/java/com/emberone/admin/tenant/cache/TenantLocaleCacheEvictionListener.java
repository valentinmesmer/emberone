/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.cache;

import com.emberone.admin.tenant.event.TenantUpdatedEvent;
import com.emberone.platform.event.AfterCommitCallback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantLocaleCacheEvictionListener {

    private final TenantLocaleCacheEvictor tenantLocaleCacheEvictor;
    private final TenantFederalStateCacheEvictor tenantFederalStateCacheEvictor;

    @AfterCommitCallback
    public void onTenantUpdated(TenantUpdatedEvent event) {
        if (event.tenantId() != null) {
            tenantLocaleCacheEvictor.evict(event.tenantId());
            tenantFederalStateCacheEvictor.evict(event.tenantId());
        }
    }
}
