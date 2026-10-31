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
public class UpdateTenantQualificationDefinitionRequest {

    @NotBlank private String name;

    @NotBlank private String abbreviation;

    private String description;

    private boolean requiresOrgDecision;

    @NotNull private QualificationRequirementMode trainingCourseRequirementMode;

    @NotNull private QualificationRequirementMode certRequirementMode;

    @NotNull private QualificationRequirementMode drivingLicenseRequirementMode;

    private List<String> trainingCourseConfigKeys;

    private List<String> certDefinitionConfigKeys;

    private List<String> drivingLicenseDefinitionConfigKeys;

    private boolean active;
}
