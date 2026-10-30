/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DrivingLicensePendingWorkflowDto {
    Long workflowId;
    LocalDate issueDate;
    LocalDate documentExpirationDate;
    String licenseNumber;
    String issuingAuthority;
    List<DrivingLicenseClassInput> classes;
}
