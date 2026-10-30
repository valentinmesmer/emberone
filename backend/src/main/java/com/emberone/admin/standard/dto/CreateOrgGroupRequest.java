/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.standard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Add an organization group template to a custom standard. */
@Data
public class CreateOrgGroupRequest {

    @NotBlank(message = "name is required") private String name;

    private String description;

    /** {@code OrganizationGroupType} name: LEADERSHIP, ALL_MEMBERS or CUSTOM. */
    @NotBlank(message = "type is required") private String type;
}
