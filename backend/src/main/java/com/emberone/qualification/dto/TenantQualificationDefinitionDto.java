/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.dto;

import com.emberone.qualification.domain.QualificationRequirementMode;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TenantQualificationDefinitionDto {
    Long id;
    String configKey;
    String abbreviation;
    String name;
    String description;
    boolean requiresOrgDecision;
    QualificationRequirementMode trainingCourseRequirementMode;
    QualificationRequirementMode certRequirementMode;
    QualificationRequirementMode drivingLicenseRequirementMode;
    List<String> trainingCourseConfigKeys;
    List<String> certDefinitionConfigKeys;
    List<String> drivingLicenseDefinitionConfigKeys;
    boolean active;
    boolean archived;
    boolean deletable;
    boolean archivable;
}
