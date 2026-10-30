/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.dto;

import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TenantCertDefinitionDto {
    Long id;
    String configKey;
    String abbreviation;
    String name;
    String description;
    Integer intervalDays;
    Integer reminderDaysBeforeExpiry;
    boolean active;
    boolean archived;
    boolean deletable;
    boolean archivable;
    Set<String> evidenceTypes;
}
