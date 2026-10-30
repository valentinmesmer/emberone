/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.dto;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CertificationDefinitionDto {
    private Long id;
    private String name;
    private String description;
    private Integer intervalDays;
    private Integer reminderDaysBeforeExpiry;
    private boolean active;
    private String configKey;
    private Set<String> evidenceTypes;
}
