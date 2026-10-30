/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class AuthentikApiTokenFeignRequestInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final String apiToken;

    @Override
    public void apply(RequestTemplate template) {
        if (apiToken != null && !apiToken.isBlank()) {
            template.header(AUTHORIZATION_HEADER, BEARER_PREFIX + apiToken);
        } else {
            log.warn("No Authentik API token available for request to: {}", template.url());
        }
    }
}
