/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.config;

import com.emberone.platform.cache.CacheNames;
import com.emberone.platform.cache.RedisCacheSerializerFactory;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
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
            RedisCacheConfiguration redisCacheConfiguration) {
        return builder ->
                builder.withCacheConfiguration(
                                CacheNames.USER_PERMISSIONS,
                                redisCacheConfiguration.entryTtl(Duration.ofMinutes(5)))
                        .withCacheConfiguration(
                                CacheNames.ORG_IN_TENANT,
                                redisCacheConfiguration.entryTtl(Duration.ofMinutes(60)))
                        .withCacheConfiguration(
                                CacheNames.EFFECTIVE_STANDARD_BY_ORG_TYPE,
                                redisCacheConfiguration.entryTtl(Duration.ofMinutes(30)))
                        .withCacheConfiguration(
                                CacheNames.ORGANIZATIONS_FOR_STANDARD,
                                redisCacheConfiguration.entryTtl(Duration.ofMinutes(30)))
                        .withCacheConfiguration(
                                CacheNames.EFFECTIVE_STANDARD_BY_ORG,
                                redisCacheConfiguration.entryTtl(Duration.ofMinutes(30)))
                        .withCacheConfiguration(
                                CacheNames.ENABLED_MODULES_BY_STANDARD,
                                redisCacheConfiguration.entryTtl(Duration.ofMinutes(30)))
                        .withCacheConfiguration(
                                CacheNames.TENANT_LOCALE,
                                redisCacheConfiguration.entryTtl(Duration.ofHours(24)))
                        .withCacheConfiguration(
                                CacheNames.TENANT_FEDERAL_STATE,
                                redisCacheConfiguration.entryTtl(Duration.ofHours(24)))
                        .withCacheConfiguration(
                                CacheNames.ACTIVE_ORG_MEMBERSHIP,
                                redisCacheConfiguration.entryTtl(Duration.ofMinutes(5)))
                        .withCacheConfiguration(
                                CacheNames.USER_ACTIVE,
                                redisCacheConfiguration.entryTtl(Duration.ofSeconds(60)));
    }
}
