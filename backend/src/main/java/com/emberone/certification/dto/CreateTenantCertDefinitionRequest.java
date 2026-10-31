/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.certification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.Data;

@Data
public class CreateTenantCertDefinitionRequest {

    @NotBlank private String abbreviation;

    @NotBlank private String name;

    private String description;

    private Integer intervalDays;

    @NotNull private Integer reminderDaysBeforeExpiry;

    private boolean active = true;

    /** Empty set = no evidence required. */
    @NotNull private Set<String> evidenceTypes;
}
