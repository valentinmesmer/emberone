/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.client;

import static org.assertj.core.api.Assertions.assertThat;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

class AuthentikApiTokenFeignRequestInterceptorTest {

    @Test
    void addsBearerHeaderWhenTokenConfigured() {
        RequestTemplate template = new RequestTemplate();

        AuthentikApiTokenFeignRequestInterceptor.builder()
                .apiToken("secret")
                .build()
                .apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer secret");
    }

    @Test
    void leavesAuthorizationHeaderAbsentForBlankToken() {
        RequestTemplate template = new RequestTemplate();

        AuthentikApiTokenFeignRequestInterceptor.builder().apiToken(" ").build().apply(template);

        assertThat(template.headers()).doesNotContainKey("Authorization");
    }
}
