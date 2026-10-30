/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.dto;

import com.emberone.platform.validation.OrganizationCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateOrganizationRequest {

    @NotBlank(message = "Name is required") @Size(max = 255, message = "Name must not exceed 255 characters") private String name;

    @NotBlank(message = "Organization code is required") @OrganizationCode
    private String organizationCode;
}
