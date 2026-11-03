/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class CompleteAdminOnboardingRequest {
    @NotBlank String adminRecordReason;
}
