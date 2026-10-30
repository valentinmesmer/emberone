/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.testsupport;

import com.emberone.platform.exceptionhandler.CoreGlobalExceptionHandler;
import com.emberone.platform.logging.RequestIdFilter;
import com.emberone.platform.security.*;
import com.emberone.user.service.UserService;
import com.emberone.workflow.onboardingsubmission.OnboardingSubmissionWorkflowOpenAccess;
import com.emberone.workflow.organizationmembershipend.OrganizationMembershipEndRequestWorkflowOpenAccess;
import com.emberone.workflow.organizationusermainorganizationchange.OrganizationUserMainOrganizationChangeWorkflowOpenAccess;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@TestConfiguration
@Import({SecurityConfig.class, CoreGlobalExceptionHandler.class})
@TestPropertySource(
        properties = {
            "spring.main.allow-bean-definition-overriding=true",
            "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://test.example.com/realms/emberone"
        })
public class SecurityWebMvcTestConfig {

    @Bean
    RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }

    @Bean
    TenantContextWebFilter tenantContextWebFilter() {
        return new TenantContextWebFilter();
    }

    @Bean
    @Primary
    JwtDecoder jwtDecoder() {
        return token ->
                Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .claim("app_role", "USER")
                        .claim("tenantId", "1")
                        .claim("userId", "1")
                        .subject("user")
                        .build();
    }

    @Bean
    TokenService tokenService() {
        return new TokenService();
    }

    @Bean
    @Primary
    DeactivatedUserAccessService deactivatedUserAccessService() {
        DeactivatedUserAccessService service = Mockito.mock(DeactivatedUserAccessService.class);
        Mockito.when(service.isActiveUser(Mockito.anyLong())).thenReturn(true);
        return service;
    }

    @Bean
    SecurityService securityService(TokenService tokenService) {
        // Real AdminAccessService so JWT role / self-service checks work in @WebMvcTest.
        return new SecurityService(
                tokenService,
                new AdminAccessService(tokenService),
                Mockito.mock(OrganizationAccessService.class),
                Mockito.mock(WorkflowOpenAccess.class),
                Mockito.mock(WorkflowInstanceAccess.class),
                Mockito.mock(OnboardingSubmissionWorkflowOpenAccess.class),
                Mockito.mock(OrganizationMembershipEndRequestWorkflowOpenAccess.class),
                Mockito.mock(OrganizationUserMainOrganizationChangeWorkflowOpenAccess.class));
    }

    @Bean
    UserService userService() {
        return Mockito.mock(UserService.class);
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
