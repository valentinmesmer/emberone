/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Always-on binding of path {@code userId} to path {@code organizationId}. Kept separate from
 * {@link TenantAdminWebConfiguration} (admin tenant path override is always registered with MVC).
 * Ordered after tenant MDC so {@code TenantContextHolder} is populated for RLS.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(WebMvcConfigurer.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class OrganizationUserMembershipWebConfiguration implements WebMvcConfigurer {

    private final OrganizationUserMembershipInterceptor organizationUserMembershipInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(organizationUserMembershipInterceptor);
    }
}
