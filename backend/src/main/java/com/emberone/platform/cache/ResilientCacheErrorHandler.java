/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/** Logs cache backend failures and treats reads as cache misses instead of failing the request. */
@Slf4j
public class ResilientCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn(
                "Cache get failed for cache '{}' key '{}': {}",
                cache.getName(),
                key,
                exception.toString());
    }

    @Override
    public void handleCachePutError(
            RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn(
                "Cache put failed for cache '{}' key '{}': {}",
                cache.getName(),
                key,
                exception.toString());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn(
                "Cache evict failed for cache '{}' key '{}': {}",
                cache.getName(),
                key,
                exception.toString());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Cache clear failed for cache '{}': {}", cache.getName(), exception.toString());
    }
}
