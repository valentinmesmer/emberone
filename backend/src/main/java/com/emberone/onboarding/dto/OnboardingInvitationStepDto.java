/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.onboarding.dto;

import com.emberone.onboarding.domain.OnboardingStepType;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OnboardingInvitationStepDto {
    OnboardingStepType stepType;
    int sortOrder;
    Map<String, Object> config;
    boolean fulfilled;
    Map<String, Object> payload;
}
