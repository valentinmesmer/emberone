/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserCertificationAssignmentDto {
    private Long id;
    private Long userId;
    private Long responsibleOrganizationId;
    private Long tenantCertDefinitionId;
    private String certDefinitionName;
    private String source;
    private LocalDate startDate;
    private LocalDate endDate;
}
