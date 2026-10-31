/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.trainingcourse.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrainingCourseTypeDto {
    private Long id;
    private String name;
    private boolean active;
    private String configKey;
}
