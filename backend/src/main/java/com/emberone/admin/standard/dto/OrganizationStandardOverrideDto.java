/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.standard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One organization that carries an explicit per-org standard override (instead of its org-type
 * default). Listed in the standards admin menu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationStandardOverrideDto {
    private Long organizationId;
    private String organizationName;
    private String organizationCode;
    private Long organizationTypeId;
    private Long standardId;
    private String standardName;
}
