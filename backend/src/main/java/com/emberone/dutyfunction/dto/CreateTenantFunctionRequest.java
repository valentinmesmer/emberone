/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.dutyfunction.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTenantFunctionRequest {
    @NotBlank private String name;

    /** Doubles as the config key; immutable after creation. */
    @NotBlank private String abbreviation;

    private boolean active = true;
}
