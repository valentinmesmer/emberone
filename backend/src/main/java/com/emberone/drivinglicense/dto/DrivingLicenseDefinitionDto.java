/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.drivinglicense.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DrivingLicenseDefinitionDto {
    Long id;
    String name;
    boolean active;
    String configKey;
}
