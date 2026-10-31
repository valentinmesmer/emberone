/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.certification.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CertificationOverviewRowDto {
    Long userId;
    String userDisplayName;
    String firstName;
    String lastName;
    Long tenantCertDefinitionId;
    String certDefinitionName;
    String status;
    LocalDate expiryDate;
}
