/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.certification.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

@Data
public class CreateUserCertificationAssignmentRequest {

    @NotNull private Long tenantCertDefinitionId;

    private Long responsibleOrganizationId;
    private LocalDate startDate;
    private LocalDate endDate;
}
