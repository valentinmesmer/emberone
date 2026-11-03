/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.qualification.dto;

import com.emberone.qualification.domain.QualificationRequirementMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class CreateTenantQualificationDefinitionRequest {

    @NotBlank private String abbreviation;

    @NotBlank private String name;

    private String description;

    private boolean requiresOrgDecision;

    @NotNull private QualificationRequirementMode trainingCourseRequirementMode =
            QualificationRequirementMode.ALL;

    @NotNull private QualificationRequirementMode certRequirementMode = QualificationRequirementMode.ALL;

    @NotNull private QualificationRequirementMode drivingLicenseRequirementMode =
            QualificationRequirementMode.ALL;

    private List<String> trainingCourseConfigKeys;

    private List<String> certDefinitionConfigKeys;

    private List<String> drivingLicenseDefinitionConfigKeys;

    private boolean active = true;
}
