/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.standard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Create a new custom standard by cloning an existing one. */
@Data
public class CreateStandardRequest {

    @NotNull(message = "Base standard id is required") private Long baseStandardId;

    @NotBlank(message = "Name is required") @Size(max = 255, message = "Name must not exceed 255 characters") private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters") private String description;
}
