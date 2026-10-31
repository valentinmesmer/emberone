/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.onboarding.spi;

public record OnboardingStepFulfillmentContext(
        long tenantId, long userId, long organizationId, long invitationId) {}
