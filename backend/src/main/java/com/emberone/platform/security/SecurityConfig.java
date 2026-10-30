/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import com.emberone.platform.logging.RequestIdFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static List<String> resolveOidcIssuerUris(Environment environment) {
        List<String> issuerUris = new ArrayList<>();
        String primaryIssuer =
                environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");
        if (StringUtils.hasText(primaryIssuer)) {
            issuerUris.add(primaryIssuer.trim());
        }
        for (int index = 0; ; index++) {
            String additionalIssuer =
                    environment.getProperty(
                            "spring.security.oauth2.resourceserver.jwt.issuer-uris[" + index + "]");
            if (!StringUtils.hasText(additionalIssuer)) {
                break;
            }
            issuerUris.add(additionalIssuer.trim());
        }
        return issuerUris.stream().distinct().toList();
    }

    private static List<String> resolveAudiences(Environment environment) {
        List<String> audiences = new ArrayList<>();
        for (int index = 0; ; index++) {
            String audience =
                    environment.getProperty(
                            "spring.security.oauth2.resourceserver.jwt.audiences[" + index + "]");
            if (!StringUtils.hasText(audience)) {
                break;
            }
            audiences.add(audience.trim());
        }
        if (audiences.isEmpty()) {
            String raw =
                    environment.getProperty("spring.security.oauth2.resourceserver.jwt.audiences");
            if (StringUtils.hasText(raw)) {
                for (String part : raw.split(",")) {
                    if (StringUtils.hasText(part)) {
                        audiences.add(part.trim());
                    }
                }
            }
        }
        return audiences.stream().distinct().toList();
    }

    static OAuth2TokenValidator<Jwt> audienceValidator(List<String> expectedAudiences) {
        return token -> {
            List<String> tokenAudiences = token.getAudience();
            if (tokenAudiences != null
                    && expectedAudiences.stream().anyMatch(tokenAudiences::contains)) {
                return OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error =
                    new OAuth2Error(
                            OAuth2ErrorCodes.INVALID_TOKEN,
                            "The required audience is missing",
                            "https://tools.ietf.org/html/rfc6750#section-3.1");
            return OAuth2TokenValidatorResult.failure(error);
        };
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "spring.security.oauth2.resourceserver.jwt",
            name = "issuer-uri")
    public JwtDecoder jwtDecoder(Environment environment) {
        return buildJwtDecoderFromIssuers(
                resolveOidcIssuerUris(environment), resolveAudiences(environment));
    }

    JwtDecoder buildJwtDecoderFromIssuers(List<String> issuerUris) {
        return buildJwtDecoderFromIssuers(issuerUris, List.of());
    }

    JwtDecoder buildJwtDecoderFromIssuers(List<String> issuerUris, List<String> audiences) {
        if (issuerUris.isEmpty()) {
            throw new IllegalStateException("No OAuth2 issuer URIs configured");
        }
        if (issuerUris.size() == 1) {
            return createDecoderForIssuer(issuerUris.getFirst(), audiences);
        }
        List<JwtDecoder> decoders = new ArrayList<>();
        for (String issuerUri : issuerUris) {
            decoders.add(createDecoderForIssuer(issuerUri, audiences));
        }
        return new CompositeJwtDecoder(decoders);
    }

    JwtDecoder createDecoderForIssuer(String issuerUri) {
        return createDecoderForIssuer(issuerUri, List.of());
    }

    JwtDecoder createDecoderForIssuer(String issuerUri, List<String> audiences) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        if (audiences == null || audiences.isEmpty()) {
            decoder.setJwtValidator(withIssuer);
            return decoder;
        }
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator(audiences)));
        return decoder;
    }

    @Bean
    DeactivatedUserWebFilter deactivatedUserWebFilter(
            DeactivatedUserAccessService deactivatedUserAccessService) {
        return new DeactivatedUserWebFilter(deactivatedUserAccessService);
    }

    @Bean
    public SecurityFilterChain authenticatedSecurityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            RequestIdFilter requestIdFilter,
            TenantContextWebFilter tenantContextWebFilter,
            DeactivatedUserWebFilter deactivatedUserWebFilter)
            throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/swagger-resources/**",
                                                "/webjars/**",
                                                "/api/swagger-ui.html",
                                                "/api/swagger-ui",
                                                "/api/swagger-ui/",
                                                "/api/swagger-ui/index.html")
                                        .permitAll()
                                        .requestMatchers("/ws", "/ws/**")
                                        .permitAll()
                                        .requestMatchers("/api/v1/public/**")
                                        .permitAll()
                                        .requestMatchers("/api/v1/self/**", "/api/v1/auth/**")
                                        .access(
                                                (authentication, context) ->
                                                        new AuthorizationDecision(
                                                                isUserAuthentication(
                                                                        authentication)))
                                        .requestMatchers("/api/v1/**")
                                        .denyAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)));
        http.addFilterBefore(requestIdFilter, BearerTokenAuthenticationFilter.class);
        http.addFilterAfter(tenantContextWebFilter, BearerTokenAuthenticationFilter.class);
        http.addFilterAfter(deactivatedUserWebFilter, TenantContextWebFilter.class);
        return http.build();
    }

    private boolean isUserAuthentication(
            Supplier<? extends Authentication> authenticationSupplier) {
        Jwt jwt = TokenClassifier.extractJwt(authenticationSupplier.get());
        if (jwt == null) {
            return false;
        }
        return EmberOneUserTokenClaims.isValidForUserFacingApi(jwt);
    }
}
