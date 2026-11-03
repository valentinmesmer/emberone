/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.trainingcourse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTenantTrainingCourseTypeRequest {

    @NotBlank private String abbreviation;

    @NotBlank private String name;

    private boolean active = true;
}
