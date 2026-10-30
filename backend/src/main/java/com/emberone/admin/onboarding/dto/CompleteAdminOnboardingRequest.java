/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class CompleteAdminOnboardingRequest {
    @NotBlank String adminRecordReason;
}
