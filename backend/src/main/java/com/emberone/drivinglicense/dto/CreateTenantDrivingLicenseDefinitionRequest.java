/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTenantDrivingLicenseDefinitionRequest {

    @NotBlank private String abbreviation;

    @NotBlank private String name;

    private boolean active = true;
}
