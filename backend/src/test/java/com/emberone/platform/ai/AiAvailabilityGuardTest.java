/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AiAvailabilityGuardTest {

    @Test
    void offByDefault() {
        AiProperties properties = new AiProperties();
        AiAvailabilityGuard guard = new AiAvailabilityGuard(properties);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(guard.isAvailable()).isFalse();
        assertThatThrownBy(guard::require).isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void enabledWithEndpointIsAvailable() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        AiAvailabilityGuard guard = new AiAvailabilityGuard(properties);

        assertThat(guard.isAvailable()).isTrue();
        guard.require();
    }

    @Test
    void enabledWithoutEndpointIsUnavailable() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setBaseUrl(" ");
        AiAvailabilityGuard guard = new AiAvailabilityGuard(properties);

        assertThat(guard.isAvailable()).isFalse();
        assertThatThrownBy(guard::require).isInstanceOf(AiUnavailableException.class);
    }
}
