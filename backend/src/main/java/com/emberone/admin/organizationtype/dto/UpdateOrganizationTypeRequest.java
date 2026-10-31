/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.organizationtype.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateOrganizationTypeRequest {

    @Size(max = 255, message = "Name must not exceed 255 characters") private String name;

    private Boolean active;
}
