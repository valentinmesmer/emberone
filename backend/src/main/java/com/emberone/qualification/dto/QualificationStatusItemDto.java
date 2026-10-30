/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.dto;

import com.emberone.qualification.domain.QualificationExerciseStatus;
import com.emberone.qualification.domain.QualificationRequirementMode;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QualificationStatusItemDto {
    Long tenantQualificationDefinitionId;
    String configKey;
    String abbreviation;
    String name;
    QualificationExerciseStatus status;
    boolean prerequisitesMet;
    boolean trainingCoursesMet;
    boolean certDefinitionsMet;
    boolean drivingLicensesMet;
    QualificationRequirementMode trainingCourseRequirementMode;
    QualificationRequirementMode certRequirementMode;
    QualificationRequirementMode drivingLicenseRequirementMode;
    boolean requiresOrgDecision;
    boolean orgDecisionGranted;
    List<UserQualificationOrgDecisionDto> orgDecisions;
    List<QualificationPrerequisiteDetailDto> trainingCourseDetails;
    List<QualificationPrerequisiteDetailDto> certDefinitionDetails;
    List<QualificationPrerequisiteDetailDto> drivingLicenseDetails;
}
