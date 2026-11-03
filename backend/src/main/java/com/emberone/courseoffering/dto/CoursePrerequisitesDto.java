/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.courseoffering.dto;

import com.emberone.courseoffering.domain.CoursePrerequisiteKind;
import com.emberone.qualification.domain.QualificationRequirementMode;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoursePrerequisitesDto {
    private QualificationRequirementMode trainingCourseRequirementMode;
    private QualificationRequirementMode certRequirementMode;
    private QualificationRequirementMode drivingLicenseRequirementMode;
    private List<Entry> entries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        private CoursePrerequisiteKind kind;
        private String configKey;
    }
}
