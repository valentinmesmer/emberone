/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.config;

import com.emberone.platform.cache.CacheNames;
import com.emberone.platform.cache.RedisCacheSerializerFactory;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class CacheConfig {

    @Bean
    RedisCacheConfiguration redisCacheConfiguration() {
        var valueSerializer = RedisCacheSerializerFactory.createValueSerializer();
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .disableCachingNullValues();
    }

    @Bean
    RedisCacheManagerBuilderCustomizer redisCacheManagerTtlCustomizer(
            RedisCacheConfiguration redisCacheConfiguration, CacheProperties properties) {
        Map<String, RedisCacheConfiguration> perCache = new LinkedHashMap<>();
        resolveTtls(properties)
                .forEach((name, ttl) -> perCache.put(name, redisCacheConfiguration.entryTtl(ttl)));
        return builder -> builder.withInitialCacheConfigurations(perCache);
    }

    /** One TTL per cache name; throws when {@code emberone.cache.ttl} misses or adds a name. */
    static Map<String, Duration> resolveTtls(CacheProperties properties) {
        Map<String, Duration> configured = properties.getTtl();
        Set<String> unknown = new TreeSet<>(configured.keySet());
        unknown.removeAll(CacheNames.ALL);
        if (!unknown.isEmpty()) {
            throw new IllegalStateException("Unknown cache in emberone.cache.ttl: " + unknown);
        }
        Map<String, Duration> resolved = new LinkedHashMap<>();
        for (String name : CacheNames.ALL) {
            Duration ttl = configured.get(name);
            if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                throw new IllegalStateException(
                        "Missing or non-positive TTL emberone.cache.ttl." + name);
            }
            resolved.put(name, ttl);
        }
        return resolved;
    }
}
