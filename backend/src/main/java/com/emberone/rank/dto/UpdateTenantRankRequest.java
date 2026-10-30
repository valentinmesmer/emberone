/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.dto;

import lombok.Data;

@Data
public class UpdateTenantRankRequest {
    private String name;
    private String abbreviation;
    private Integer sortOrder;
    private Boolean active;
}
