/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.dto;

import com.emberone.workflow.dto.TrainingCourseRecordWorkflowPayload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class UpdateTrainingCourseUserRecordRequest {

    @Size(max = TrainingCourseRecordWorkflowPayload.MAX_NAME_LENGTH) private String name;

    @NotNull(message = "startDate is required") private LocalDate startDate;

    @NotNull(message = "endDate is required") private LocalDate endDate;

    @NotBlank(message = "location is required") @Size(max = 512) private String location;

    private BigDecimal hours;

    @Size(max = 2048) private String notes;
}
