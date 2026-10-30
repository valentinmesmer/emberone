/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.cache;

import com.emberone.platform.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class EffectiveStandardOrganizationCacheEvictor {

    @CacheEvict(
            cacheNames = CacheNames.EFFECTIVE_STANDARD_BY_ORG,
            key = "#tenantId + ':' + #organizationId")
    public void evict(Long tenantId, Long organizationId) {}
}
