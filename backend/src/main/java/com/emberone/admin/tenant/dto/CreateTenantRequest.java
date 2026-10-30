/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.dto;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.federalstate.domain.FederalState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTenantRequest {

    @NotBlank(message = "Name is required") @Size(max = 255, message = "Name must not exceed 255 characters") private String name;

    @NotBlank(message = "Tenant code is required") @Size(max = 32, message = "Tenant code must not exceed 32 characters") private String abbreviation;

    /**
     * Tenant UI language; immutable after creation. Only {@link SupportedLocale#DE} is supported
     * initially.
     */
    private SupportedLocale locale = SupportedLocale.DE;

    /**
     * German federal state; immutable after creation. Must be a configured {@link FederalState}.
     */
    private FederalState federalState = FederalState.BAYERN;
}
