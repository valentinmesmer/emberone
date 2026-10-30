/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Audience of one managing organization plus the options its editor offers. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagementAudienceSettingsDto {
    private List<ManagementAudienceEntryDto> entries;
    private List<ManagementOptionDto> organizations;
    private List<ManagementOptionDto> organizationTypes;
}
