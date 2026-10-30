/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.PropertyNamingStrategies;

class EmberOneJsonMapperTest {

    @Test
    void createBuildsMapperWithCamelCaseByDefault() throws Exception {
        var mapper = EmberOneJsonMapper.create();
        String json = mapper.writeValueAsString(new Sample("hello"));

        assertThat(json).contains("\"firstName\":\"hello\"");
    }

    @Test
    void createAcceptsCustomNamingStrategy() throws Exception {
        var mapper = EmberOneJsonMapper.create(PropertyNamingStrategies.SNAKE_CASE);
        String json = mapper.writeValueAsString(new Sample("hello"));

        assertThat(json).contains("\"first_name\":\"hello\"");
    }

    private record Sample(String firstName) {}
}
