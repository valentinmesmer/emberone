/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.standard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** Add a permission group template (with its granted permission types) to a custom standard. */
@Data
public class CreatePermissionGroupRequest {

    @NotBlank(message = "name is required") private String name;

    private String description;

    /** {@code PermissionGroupType} name; only {@code CUSTOM} is allowed when adding via the API. */
    @NotBlank(message = "type is required") private String type;

    /** Granted permission type enum names. */
    @NotNull(message = "permissionTypes is required") private List<String> permissionTypes;
}
