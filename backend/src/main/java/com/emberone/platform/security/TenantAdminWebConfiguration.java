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
 * MVC interceptors for tenant scope beyond JWT claims. Request-wide holder binding is handled by
 * {@link TenantContextWebFilter}; PostgreSQL GUCs are applied in {@link
 * TenantContextJpaTransactionManager}.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(WebMvcConfigurer.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class TenantAdminWebConfiguration implements WebMvcConfigurer {

    private final AdminPathTenantContextInterceptor adminPathTenantContextInterceptor;
    private final GlobalAdminReadScopeInterceptor globalAdminReadScopeInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminPathTenantContextInterceptor);
        registry.addInterceptor(globalAdminReadScopeInterceptor);
    }
}
