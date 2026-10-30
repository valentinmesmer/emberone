/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.dto;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.federalstate.domain.FederalState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDto {
    private Long id;
    private String name;
    private String abbreviation;
    private SupportedLocale locale;
    private FederalState federalState;
}
