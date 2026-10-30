/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.config;

import com.emberone.identity.exception.IdentityException;
import com.emberone.identity.provider.IdentityProviderKind;
import com.emberone.identity.provider.IdentityProviderUserProvisioner;
import com.emberone.identity.service.AuthentikUserService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableConfigurationProperties(IdentityProviderProperties.class)
public class IdentityProviderConfiguration {

    @Bean
    @Primary
    IdentityProviderUserProvisioner identityProviderUserProvisioner(
            IdentityProviderProperties properties,
            ObjectProvider<AuthentikUserService> authentikUserService) {
        IdentityProviderKind kind = properties.resolvedKind();
        return switch (kind) {
            case AUTHENTIK -> authentikUserService.getObject();
            default ->
                    throw new IdentityException("No IdentityProviderUserProvisioner for " + kind);
        };
    }
}
