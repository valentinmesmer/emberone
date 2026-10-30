/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import com.emberone.courseoffering.domain.CoursePrerequisiteKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CoursePrerequisiteEntryRequest {

    @NotNull(message = "Kind is required") private CoursePrerequisiteKind kind;

    @NotBlank(message = "Config key is required") private String configKey;
}
