/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SecurityConfigTest {

    private static SecurityConfig configReturningDecoder(JwtDecoder decoder) {
        return new SecurityConfig() {
            @Override
            JwtDecoder createDecoderForIssuer(String issuerUri, List<String> audiences) {
                return decoder;
            }
        };
    }

    private static Jwt userJwt(String appRole, String tenantId) {
        Jwt.Builder builder =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .claim("app_role", appRole)
                        .subject("user");
        if (tenantId != null) {
            builder.claim("tenantId", tenantId);
        }
        return builder.build();
    }

    private static boolean isUserAuthentication(Authentication authentication) throws Exception {
        Method method =
                SecurityConfig.class.getDeclaredMethod("isUserAuthentication", Supplier.class);
        method.setAccessible(true);
        SecurityConfig config = new SecurityConfig();
        return (boolean) method.invoke(config, (Supplier<Authentication>) () -> authentication);
    }

    @SuppressWarnings("unchecked")
    private static List<String> resolveOidcIssuerUris(Environment environment) throws Exception {
        Method method =
                SecurityConfig.class.getDeclaredMethod("resolveOidcIssuerUris", Environment.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(null, environment);
    }

    @Test
    void resolveOidcIssuerUrisCollectsPrimaryAndAdditionalIssuers() throws Exception {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))
                .thenReturn(" https://auth.example.com/realms/emberone ");
        when(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uris[0]"))
                .thenReturn("https://auth2.example.com/realms/emberone");
        when(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uris[1]"))
                .thenReturn(null);

        List<String> issuerUris = resolveOidcIssuerUris(environment);

        assertThat(issuerUris)
                .containsExactly(
                        "https://auth.example.com/realms/emberone",
                        "https://auth2.example.com/realms/emberone");
    }

    @Test
    void resolveOidcIssuerUrisDeduplicatesValues() throws Exception {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))
                .thenReturn("https://auth.example.com/realms/emberone");
        when(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uris[0]"))
                .thenReturn("https://auth.example.com/realms/emberone");
        when(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uris[1]"))
                .thenReturn(null);

        List<String> issuerUris = resolveOidcIssuerUris(environment);

        assertThat(issuerUris).containsExactly("https://auth.example.com/realms/emberone");
    }

    @Test
    void resolveOidcIssuerUrisCollectsOnlyAdditionalIssuers() throws Exception {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))
                .thenReturn(null);
        when(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uris[0]"))
                .thenReturn(" https://auth2.example.com/realms/emberone ");
        when(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uris[1]"))
                .thenReturn(null);

        List<String> issuerUris = resolveOidcIssuerUris(environment);

        assertThat(issuerUris).containsExactly("https://auth2.example.com/realms/emberone");
    }

    @Test
    void jwtDecoderThrowsWhenNoIssuerConfigured() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))
                .thenReturn("   ");
        when(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uris[0]"))
                .thenReturn(null);

        assertThatThrownBy(() -> new SecurityConfig().jwtDecoder(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No OAuth2 issuer URIs configured");
    }

    @Test
    void buildJwtDecoderFromIssuersUsesSingleDecoderForOneIssuer() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        SecurityConfig config = configReturningDecoder(decoder);

        assertThat(
                        config.buildJwtDecoderFromIssuers(
                                List.of("https://auth.example.com/realms/emberone")))
                .isSameAs(decoder);
    }

    @Test
    void buildJwtDecoderFromIssuersUsesCompositeDecoderForMultipleIssuers() {
        SecurityConfig config = configReturningDecoder(mock(JwtDecoder.class));

        JwtDecoder decoder =
                config.buildJwtDecoderFromIssuers(
                        List.of(
                                "https://auth.example.com/realms/emberone",
                                "https://auth2.example.com/realms/emberone"));

        assertThat(decoder).isInstanceOf(CompositeJwtDecoder.class);
    }

    @Test
    void buildJwtDecoderFromIssuersRejectsEmptyIssuerList() {
        assertThatThrownBy(() -> new SecurityConfig().buildJwtDecoderFromIssuers(List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No OAuth2 issuer URIs configured");
    }

    @Test
    void audienceValidatorAcceptsMatchingAudience() {
        Jwt jwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .audience(List.of("emberone"))
                        .subject("user")
                        .build();

        assertThat(SecurityConfig.audienceValidator(List.of("emberone")).validate(jwt).hasErrors())
                .isFalse();
    }

    @Test
    void audienceValidatorRejectsMissingAudience() {
        Jwt jwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .audience(List.of("other-client"))
                        .subject("user")
                        .build();

        assertThat(SecurityConfig.audienceValidator(List.of("emberone")).validate(jwt).hasErrors())
                .isTrue();
    }

    @Test
    void userFacingApiAccessAllowsTenantUserWithAppRole() throws Exception {
        Jwt jwt = userJwt("USER", "42");
        Authentication authentication = new JwtAuthenticationToken(jwt);

        assertThat(isUserAuthentication(authentication)).isTrue();
    }

    @Test
    void userFacingApiAccessAllowsGlobalAdminWithoutTenant() throws Exception {
        Jwt jwt = userJwt("GLOBAL_ADMIN", null);
        Authentication authentication = new JwtAuthenticationToken(jwt);

        assertThat(isUserAuthentication(authentication)).isTrue();
    }

    @Test
    void userFacingApiAccessAllowsTenantAdminWithTenant() throws Exception {
        Jwt jwt = userJwt("TENANT_ADMIN", "42");
        Authentication authentication = new JwtAuthenticationToken(jwt);

        assertThat(isUserAuthentication(authentication)).isTrue();
    }

    @Test
    void userFacingApiAccessRejectsTokenWithoutAppRole() throws Exception {
        Jwt jwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .claim("tenantId", "42")
                        .subject("user")
                        .build();
        Authentication authentication = new JwtAuthenticationToken(jwt);

        assertThat(isUserAuthentication(authentication)).isFalse();
    }

    @Test
    void userFacingApiAccessRejectsUserTokenWithoutTenant() throws Exception {
        Jwt jwt = userJwt("USER", null);
        Authentication authentication = new JwtAuthenticationToken(jwt);

        assertThat(isUserAuthentication(authentication)).isFalse();
    }

    @Test
    void userFacingApiAccessRejectsNonJwtAuthentication() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "secret");

        assertThat(isUserAuthentication(authentication)).isFalse();
    }
}
