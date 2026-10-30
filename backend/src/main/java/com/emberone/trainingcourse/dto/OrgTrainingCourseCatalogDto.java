/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrgTrainingCourseCatalogDto {
    private Long organizationId;
    private List<TrainingCourseTypeDto> types;
}
