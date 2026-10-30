/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.cache;

import com.emberone.platform.cache.CacheNames;
import com.emberone.platform.cache.value.UserActiveCacheValue;
import com.emberone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserActiveCache {

    private final UserRepository userRepository;

    @Cacheable(cacheNames = CacheNames.USER_ACTIVE, key = "#userId", unless = "!#result.active()")
    public UserActiveCacheValue loadCached(Long userId) {
        return new UserActiveCacheValue(userRepository.existsByIdAndDeactivatedAtIsNull(userId));
    }
}
