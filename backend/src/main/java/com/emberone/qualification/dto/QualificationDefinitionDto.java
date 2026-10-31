/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.qualification.dto;

import com.emberone.qualification.domain.QualificationRequirementMode;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QualificationDefinitionDto {
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
}
