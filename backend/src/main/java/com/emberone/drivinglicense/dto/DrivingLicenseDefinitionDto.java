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
public class DrivingLicenseDefinitionDto {
    Long id;
    String name;
    boolean active;
    String configKey;
}
