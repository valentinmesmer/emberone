/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

@Data
public class CreateQualificationOrgDecisionRequest {

    @NotNull private LocalDate startDate;

    private LocalDate endDate;

    /** When set, updates an existing interval instead of creating a new one. */
    private Long id;

    private String notes;
}
