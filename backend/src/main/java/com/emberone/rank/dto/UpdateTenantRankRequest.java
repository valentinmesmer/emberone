/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.rank.dto;

import lombok.Data;

@Data
public class UpdateTenantRankRequest {
    private String name;
    private Integer sortOrder;
    private Boolean active;
}
