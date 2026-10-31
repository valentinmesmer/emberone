/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.standard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Map an organization group to a permission group within a custom standard. */
@Data
public class CreateGroupMappingRequest {

    @NotBlank(message = "orgGroupConfigKey is required") private String orgGroupConfigKey;

    @NotBlank(message = "permissionGroupConfigKey is required") private String permissionGroupConfigKey;
}
