/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.organization.cache;

import com.emberone.platform.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class OrganizationTenantCacheEvictor {

    @CacheEvict(cacheNames = CacheNames.ORG_IN_TENANT, key = "#tenantId + ':' + #organizationId")
    public void evict(Long organizationId, Long tenantId) {}
}
