/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.time;

import java.time.ZoneId;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(OrganizationTimeProperties.class)
public class OrganizationTimeConfiguration implements WebMvcConfigurer {

    @Bean
    OrganizationWallClockTimeInitializer organizationWallClockTimeInitializer(
            OrganizationTimeProperties properties) {
        ZoneId zoneId = ZoneId.of(properties.getOrganizationZone());
        OrganizationWallClockTime.configure(zoneId);
        return new OrganizationWallClockTimeInitializer(zoneId);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(new OrganizationWallClockInstantFormatter());
    }

    record OrganizationWallClockTimeInitializer(ZoneId zoneId) {}
}
