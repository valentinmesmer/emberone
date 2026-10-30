/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
