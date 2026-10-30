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
public class DrivingLicenseDto {
    Long id;
    Long userId;
    LocalDate issueDate;
    LocalDate documentExpirationDate;
    String licenseNumber;
    String issuingAuthority;
    List<Long> userDocumentIds;
    List<DrivingLicenseClassDto> classes;
    String notes;
    String appLinkType;
    Long appLinkId;
    String navigationPath;
}
