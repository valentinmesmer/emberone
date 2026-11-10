/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis cache settings. {@code emberone.cache.ttl} maps every cache name from {@link
 * com.emberone.platform.cache.CacheNames} to its entry TTL; {@link CacheConfig} fails the start
 * when a name is missing or unknown, so a new cache cannot go live without a deliberate TTL.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "emberone.cache")
public class CacheProperties {

    private Map<String, Duration> ttl = new LinkedHashMap<>();
}
