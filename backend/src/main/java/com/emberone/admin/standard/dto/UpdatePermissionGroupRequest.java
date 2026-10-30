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

/** Update name/description and granted permission types of a permission group template. */
@Data
public class UpdatePermissionGroupRequest {

    @NotBlank(message = "name is required") private String name;

    private String description;

    /** Granted permission type enum names; the full set replaces the existing grants. */
    @NotNull(message = "permissionTypes is required") private List<String> permissionTypes;
}
