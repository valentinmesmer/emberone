/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.logging.RequestIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SecurityFilterChainTest.TestConfig.class)
@WebAppConfiguration
class SecurityFilterChainTest {

    @Autowired WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void swaggerUiIsPublicWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }

    @Test
    void apiV1IsDeniedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/organizations")).andExpect(status().isUnauthorized());
    }

    @Test
    void publicNotificationEmailVerifyIsAllowedWithoutAuthentication() throws Exception {
        mockMvc.perform(
                        post("/api/v1/public/notification-email/verify")
                                .contentType("application/json")
                                .content("{\"token\":\"abc\",\"tenantId\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unmappedPathsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/internal/health")).andExpect(status().isUnauthorized());
    }

    @Configuration
    @Import(SecurityConfig.class)
    static class TestConfig {

        @Bean
        RequestIdFilter requestIdFilter() {
            return new RequestIdFilter();
        }

        @Bean
        TenantContextWebFilter tenantContextWebFilter() {
            return new TenantContextWebFilter();
        }

        @Bean
        DeactivatedUserWebFilter deactivatedUserWebFilter() {
            DeactivatedUserAccessService accessService =
                    Mockito.mock(DeactivatedUserAccessService.class);
            Mockito.when(accessService.isActiveUser(Mockito.anyLong())).thenReturn(true);
            return new DeactivatedUserWebFilter(accessService);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token ->
                    Jwt.withTokenValue(token)
                            .header("alg", "none")
                            .claim("app_role", "USER")
                            .claim("tenantId", "1")
                            .subject("user")
                            .build();
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.applyPermitDefaultValues();
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return source;
        }
    }
}
