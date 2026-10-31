/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.standard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Update name/description of an organization group template within a custom standard. */
@Data
public class UpdateOrgGroupRequest {

    @NotBlank(message = "name is required") private String name;

    private String description;
}
