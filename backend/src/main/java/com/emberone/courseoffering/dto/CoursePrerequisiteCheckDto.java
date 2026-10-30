/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import com.emberone.qualification.domain.QualificationRequirementMode;
import com.emberone.qualification.dto.QualificationPrerequisiteDetailDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** How a user stands against the prerequisites of an offering. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoursePrerequisiteCheckDto {
    private boolean met;
    private QualificationRequirementMode trainingCourseRequirementMode;
    private QualificationRequirementMode certRequirementMode;
    private QualificationRequirementMode drivingLicenseRequirementMode;
    private List<QualificationPrerequisiteDetailDto> trainingCourseDetails;
    private List<QualificationPrerequisiteDetailDto> certDefinitionDetails;
    private List<QualificationPrerequisiteDetailDto> drivingLicenseDetails;
}
