/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.dutyfunction.dto;

import lombok.Data;

@Data
public class UpdateTenantFunctionRequest {
    private String name;
    private String abbreviation;
    private Integer sortOrder;
    private Boolean active;
}
