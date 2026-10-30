/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.config;

import com.emberone.identity.provider.IdentityProviderKind;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "emberone.identity")
public class IdentityProviderProperties {

    private String deprovisionCron = "0 * * * * *";
    private int deprovisionBatchSize = 100;
    private int maxDeprovisionAttempts = 5;
    private Duration deprovisionProcessingStaleAfter = Duration.ofMinutes(15);
    private Duration deprovisionRetryBackoff = Duration.ofMinutes(1);

    /** Hardcoded until multiple IdPs are selectable via config. */
    public IdentityProviderKind resolvedKind() {
        return IdentityProviderKind.AUTHENTIK;
    }
}
