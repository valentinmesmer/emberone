/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.dto;

import com.emberone.qualification.domain.QualificationOrgDecisionStatus;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserQualificationOrgDecisionDto {
    Long id;
    QualificationOrgDecisionStatus status;
    LocalDate startDate;
    LocalDate endDate;
    String notes;
}
