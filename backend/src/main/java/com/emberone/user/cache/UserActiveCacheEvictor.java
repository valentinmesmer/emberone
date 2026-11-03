/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.user.cache;

import com.emberone.platform.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class UserActiveCacheEvictor {

    @CacheEvict(cacheNames = CacheNames.USER_ACTIVE, key = "#userId")
    public void evict(Long userId) {}
}
