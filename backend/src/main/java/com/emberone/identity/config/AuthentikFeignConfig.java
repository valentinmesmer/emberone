/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.config;

import com.emberone.identity.client.AuthentikApiClient;
import com.emberone.identity.client.AuthentikApiTokenFeignRequestInterceptor;
import com.emberone.platform.config.EmberOneJsonMapper;
import feign.Feign;
import feign.Request;
import feign.Retryer;
import feign.jackson3.Jackson3Decoder;
import feign.jackson3.Jackson3Encoder;
import feign.okhttp.OkHttpClient;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@ConditionalOnProperty(
        prefix = "emberone.identity",
        name = "provider",
        havingValue = "authentik",
        matchIfMissing = true)
public class AuthentikFeignConfig {

    @Bean(name = "authentikApiInterceptor")
    public AuthentikApiTokenFeignRequestInterceptor authentikApiInterceptor(
            @Value("${authentik.api.token:}") String apiToken) {
        return AuthentikApiTokenFeignRequestInterceptor.builder()
                .apiToken(apiToken != null ? apiToken : "")
                .build();
    }

    @Bean
    public AuthentikApiClient authentikApiClient(
            @Value("${authentik.api.base-url}") String authentikApiBaseUrl,
            @Autowired(required = false) @Qualifier("authentikApiInterceptor") AuthentikApiTokenFeignRequestInterceptor interceptor) {
        JsonMapper jsonMapper = EmberOneJsonMapper.create(PropertyNamingStrategies.SNAKE_CASE);
        Feign.Builder builder =
                Feign.builder()
                        .client(new OkHttpClient())
                        .options(
                                new Request.Options(2, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true))
                        .retryer(Retryer.NEVER_RETRY)
                        .encoder(new Jackson3Encoder(jsonMapper))
                        .decoder(new Jackson3Decoder(jsonMapper));
        if (interceptor != null) {
            builder.requestInterceptor(interceptor);
        }
        return builder.target(AuthentikApiClient.class, authentikApiBaseUrl);
    }
}
