/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.platform.cache.CacheNames;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

class CacheConfigTest {

    private static CacheProperties applicationYmlProperties() throws Exception {
        List<PropertySource<?>> sources =
                new YamlPropertySourceLoader()
                        .load(
                                "application",
                                new FileSystemResource(
                                        Path.of("src/main/resources/application.yml")));
        Binder binder = new Binder(ConfigurationPropertySources.from(sources));
        return binder.bind("emberone.cache", CacheProperties.class).get();
    }

    @Test
    void applicationYmlDefinesAPositiveTtlForEveryCache() throws Exception {
        Map<String, Duration> ttls = CacheConfig.resolveTtls(applicationYmlProperties());

        assertThat(ttls).containsOnlyKeys(CacheNames.ALL);
        assertThat(ttls.get(CacheNames.USER_ACTIVE)).isEqualTo(Duration.ofSeconds(60));
        assertThat(ttls.get(CacheNames.TENANT_LOCALE)).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void missingCacheFailsTheStart() {
        CacheProperties properties = new CacheProperties();

        assertThatThrownBy(() -> CacheConfig.resolveTtls(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(CacheNames.ALL.get(0));
    }

    @Test
    void unknownCacheFailsTheStart() throws Exception {
        CacheProperties properties = applicationYmlProperties();
        properties.getTtl().put("typoCache", Duration.ofMinutes(1));

        assertThatThrownBy(() -> CacheConfig.resolveTtls(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("typoCache");
    }
}
