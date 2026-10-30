/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.dto;

import com.emberone.workflow.dto.TrainingCourseRecordWorkflowPayload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/** Manually create a training course record for a user ({@code appLinkType} stays null). */
@Data
public class CreateTrainingCourseUserRecordRequest {

    @Size(max = TrainingCourseRecordWorkflowPayload.MAX_NAME_LENGTH) private String name;

    @NotEmpty(message = "tenantTrainingCourseTypeIds is required") @Size(max = TrainingCourseRecordWorkflowPayload.MAX_GRANTED_TYPES) private List<@NotNull Long> tenantTrainingCourseTypeIds;

    @NotNull(message = "startDate is required") private LocalDate startDate;

    @NotNull(message = "endDate is required") private LocalDate endDate;

    @NotBlank(message = "location is required") @Size(max = 512) private String location;

    private BigDecimal hours;

    private List<Long> userDocumentIds;

    private String notes;
}
