/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.config;

/** Marker config for steps without admin-configurable payload. */
public record EmptyStepConfig() implements OnboardingStepConfig {
    public static final EmptyStepConfig INSTANCE = new EmptyStepConfig();
}
