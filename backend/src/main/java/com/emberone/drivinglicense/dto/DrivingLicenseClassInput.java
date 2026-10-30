/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

@Data
public class DrivingLicenseClassInput {

    @NotNull(message = "tenantDrivingLicenseDefinitionId is required") private Long tenantDrivingLicenseDefinitionId;

    private LocalDate grantDate;

    private LocalDate expirationDate;
}
