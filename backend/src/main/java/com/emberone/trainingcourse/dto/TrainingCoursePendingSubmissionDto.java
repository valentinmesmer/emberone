/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrainingCoursePendingSubmissionDto {
    private Long workflowId;
    private String name;
    private List<Long> tenantTrainingCourseTypeIds;
    private List<String> courseTypeNames;
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;
    private BigDecimal hours;
}
