/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.standard.cache;

import com.emberone.platform.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class EffectiveStandardTypeCacheEvictor {

    @CacheEvict(
            cacheNames = CacheNames.EFFECTIVE_STANDARD_BY_ORG_TYPE,
            key = "#tenantId + ':' + #organizationTypeId")
    public void evict(Long tenantId, Long organizationTypeId) {}
}
