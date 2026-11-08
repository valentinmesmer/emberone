/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Refuses AI calls while the feature is switched off or no endpoint is configured. Single place
 * deciding whether AI is available; the capabilities endpoint reads the same answer so the UI never
 * offers a refused function. Which model endpoint may receive which data is the operator's
 * responsibility.
 */
@Component
@RequiredArgsConstructor
public class AiAvailabilityGuard {

    private final AiProperties properties;

    public boolean isAvailable() {
        return properties.isEnabled() && StringUtils.hasText(properties.getBaseUrl());
    }

    public void require() {
        if (!isAvailable()) {
            throw new AiUnavailableException("AI assistance is not enabled");
        }
    }
}
