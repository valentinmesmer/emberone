/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TenantDrivingLicenseDefinitionDto {
    Long id;
    String configKey;
    String abbreviation;
    String name;
    boolean active;
    boolean archived;
    boolean deletable;
    boolean archivable;
}
