/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.cache;

import com.emberone.platform.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class OrganizationTenantCacheEvictor {

    @CacheEvict(cacheNames = CacheNames.ORG_IN_TENANT, key = "#organizationId + ':' + #tenantId")
    public void evict(Long organizationId, Long tenantId) {}
}
