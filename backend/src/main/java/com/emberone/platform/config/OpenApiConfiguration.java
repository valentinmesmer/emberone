/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single OpenAPI document for the monolith: springdoc scans all {@code com.emberone.*} controllers.
 */
@Configuration
public class OpenApiConfiguration {

    private static final String JWT_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI emberoneOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("EmberOne API")
                                .description(
                                        "Auto-generated from Spring MVC controllers (springdoc-openapi).")
                                .version("1"))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        JWT_SCHEME,
                                        new SecurityScheme()
                                                .name(JWT_SCHEME)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description(
                                                        "OIDC access token (e.g. Authentik) with audience/claims expected by the resource server.")));
    }
}
