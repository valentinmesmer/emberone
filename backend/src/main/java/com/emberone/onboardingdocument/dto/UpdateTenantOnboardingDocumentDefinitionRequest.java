/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.dto;

import com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTenantOnboardingDocumentDefinitionRequest {

    @NotBlank private String abbreviation;

    @NotBlank private String name;

    private String description = "";

    @NotNull private OnboardingDocumentFulfillmentMode fulfillmentMode;

    private boolean mandatory;

    private boolean active;
}
