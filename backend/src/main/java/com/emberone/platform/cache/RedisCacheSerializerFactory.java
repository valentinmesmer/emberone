/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.cache;

import com.emberone.platform.config.EmberOneJsonMapper;
import lombok.experimental.UtilityClass;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * Redis cache values are serialized as JSON with default typing so {@code deserialize(bytes)} can
 * reconstruct concrete types. Domain scalars (enums, booleans) must be wrapped in {@link
 * com.emberone.platform.cache.value} records - bare scalars lose type information.
 */
@UtilityClass
public class RedisCacheSerializerFactory {

    public static GenericJacksonJsonRedisSerializer createValueSerializer() {
        var typeValidator =
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.emberone.")
                        .allowIfSubType("java.util.")
                        .allowIfSubType("java.lang.")
                        .build();
        return GenericJacksonJsonRedisSerializer.builder(EmberOneJsonMapper::newBuilder)
                .enableDefaultTyping(typeValidator)
                .build();
    }
}
