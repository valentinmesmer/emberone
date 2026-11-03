/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.onboarding.config;

/** Marker config for steps without admin-configurable payload. */
public record EmptyStepConfig() implements OnboardingStepConfig {
    public static final EmptyStepConfig INSTANCE = new EmptyStepConfig();
}
