/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permission.cache;

import com.emberone.platform.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class UserPermissionCacheEvictor {

    @CacheEvict(cacheNames = CacheNames.USER_PERMISSIONS, key = "#tenantId + ':' + #userId")
    public void evictUser(Long tenantId, Long userId) {}
}
