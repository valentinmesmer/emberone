/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.onboarding.dto;

import com.emberone.onboarding.domain.OnboardingStepType;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class OnboardingStepTypeDto {
    OnboardingStepType stepType;
}
