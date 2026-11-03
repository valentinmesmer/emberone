/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.management.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Audience of one managing organization plus the options its editor offers. {@code enabled} is
 * false when the management module is off for the organization's standard; the selection is then
 * neither shown nor writable (same pattern as course-offering types).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagementAudienceSettingsDto {
    private boolean enabled;
    private List<ManagementAudienceEntryDto> entries;
    private List<ManagementOptionDto> organizations;
    private List<ManagementOptionDto> organizationTypes;
}
