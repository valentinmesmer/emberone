/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration
public class JacksonAutoConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean(JsonMapper.class)
    public JsonMapper jsonMapper() {
        return EmberOneJsonMapper.create();
    }
}
