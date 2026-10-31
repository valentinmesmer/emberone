/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.trainingcourse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTenantTrainingCourseTypeRequest {

    @NotBlank private String name;

    @NotBlank private String abbreviation;

    private boolean active = true;
}
