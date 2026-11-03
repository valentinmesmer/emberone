/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.trainingcourse.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TenantTrainingCourseTypeDto {
    Long id;
    String configKey;
    String abbreviation;
    String name;
    boolean active;
    boolean archived;
    boolean deletable;
    boolean archivable;
}
