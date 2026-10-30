/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.cache;

import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.cache.CacheNames;
import com.emberone.platform.cache.value.OrganizationInTenantCacheValue;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationTenantCache {

    private final OrganizationRepository organizationRepository;

    @Cacheable(cacheNames = CacheNames.ORG_IN_TENANT, key = "#organizationId + ':' + #tenantId")
    public OrganizationInTenantCacheValue loadCached(Long organizationId, Long tenantId) {
        boolean exists = organizationRepository.existsByIdAndTenantId(organizationId, tenantId);
        return new OrganizationInTenantCacheValue(exists);
    }
}
