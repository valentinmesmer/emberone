/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.standard.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignStandardRequest {

    @NotNull(message = "Standard id is required") private Long standardId;
}
