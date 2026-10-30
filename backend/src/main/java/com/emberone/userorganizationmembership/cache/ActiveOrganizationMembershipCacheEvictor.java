/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.cache;

import com.emberone.platform.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class ActiveOrganizationMembershipCacheEvictor {

    @CacheEvict(
            cacheNames = CacheNames.ACTIVE_ORG_MEMBERSHIP,
            key = "#userId + ':' + #organizationId")
    public void evict(Long userId, Long organizationId) {}
}
