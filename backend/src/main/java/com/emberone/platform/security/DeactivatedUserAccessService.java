/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import com.emberone.user.cache.UserActiveCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Per-request deactivation check. Backed by the Redis cache {@code userActive} (shared by all
 * instances), evicted when a user is deactivated; the short TTL bounds staleness if an eviction is
 * lost.
 */
@Service
@RequiredArgsConstructor
public class DeactivatedUserAccessService {

    private final UserActiveCache userActiveCache;

    public boolean isActiveUser(Long userId) {
        return userActiveCache.loadCached(userId).active();
    }
}
