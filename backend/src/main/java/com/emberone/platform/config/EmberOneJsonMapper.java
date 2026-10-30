/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.config;

import lombok.experimental.UtilityClass;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.json.JsonMapper;

@UtilityClass
public class EmberOneJsonMapper {

    public static JsonMapper create() {
        return create(PropertyNamingStrategies.LOWER_CAMEL_CASE);
    }

    public static JsonMapper create(PropertyNamingStrategy namingStrategy) {
        return newBuilder(namingStrategy).build();
    }

    public static JsonMapper.Builder newBuilder() {
        return newBuilder(PropertyNamingStrategies.LOWER_CAMEL_CASE);
    }

    public static JsonMapper.Builder newBuilder(PropertyNamingStrategy namingStrategy) {
        return JsonMapper.builder()
                .propertyNamingStrategy(namingStrategy)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .findAndAddModules();
    }
}
