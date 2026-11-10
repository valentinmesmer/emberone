/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userpermission.cache;

import com.emberone.platform.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class UserPermissionCacheEvictor {

    @CacheEvict(cacheNames = CacheNames.USER_PERMISSIONS, key = "#tenantId + ':' + #userId")
    public void evictUser(Long tenantId, Long userId) {}
}
