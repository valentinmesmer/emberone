/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.organization.cache;

import com.emberone.organization.event.OrganizationCreatedEvent;
import com.emberone.platform.event.AfterCommitCallback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationCacheEvictionListener {

    private final OrganizationTenantCacheEvictor organizationTenantCacheEvictor;

    @AfterCommitCallback
    public void onOrganizationCreated(OrganizationCreatedEvent event) {
        if (event.tenantId() == null || event.organizationId() == null) {
            return;
        }
        organizationTenantCacheEvictor.evict(event.organizationId(), event.tenantId());
    }
}
