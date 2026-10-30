/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
