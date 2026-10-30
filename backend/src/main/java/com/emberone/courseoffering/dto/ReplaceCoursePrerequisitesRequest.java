/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import com.emberone.qualification.domain.QualificationRequirementMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class ReplaceCoursePrerequisitesRequest {

    @NotNull private QualificationRequirementMode trainingCourseRequirementMode;

    @NotNull private QualificationRequirementMode certRequirementMode;

    @NotNull private QualificationRequirementMode drivingLicenseRequirementMode;

    @NotNull private List<@Valid CoursePrerequisiteEntryRequest> entries;
}
