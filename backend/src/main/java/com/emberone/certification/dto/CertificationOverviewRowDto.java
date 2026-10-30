/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
