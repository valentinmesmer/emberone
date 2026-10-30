/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DrivingLicenseClassDto {
    Long tenantDrivingLicenseDefinitionId;
    String className;
    String abbreviation;
    boolean definitionArchived;
    LocalDate grantDate;
    LocalDate expirationDate;
}
