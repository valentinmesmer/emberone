/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Health and Prometheus on {@code management.server.port} must stay reachable for probes without a
 * JWT. When the management port differs from the app port, Boot serves actuator only there; this
 * chain still permits the same paths if actuator is ever hit on the main port.
 */
@Configuration
public class ManagementPortSecurityConfig {

    @Bean
    @Order(0)
    SecurityFilterChain actuatorEndpointsSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(
                        "/actuator/health",
                        "/actuator/health/**",
                        "/actuator/prometheus",
                        "/actuator/prometheus/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
