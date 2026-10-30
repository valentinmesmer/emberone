/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for {@link com.emberone.workflow.domain.WorkflowType#TRAINING_COURSE_RECORD}: the granted
 * training course types, date range, optional hours and notes, and proof document drafts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingCourseRecordWorkflowPayload {

    public static final int MAX_ATTACHMENTS = 10;
    public static final int MAX_GRANTED_TYPES = 20;
    public static final int MAX_NAME_LENGTH = 256;

    @NotEmpty @Size(max = MAX_GRANTED_TYPES) private List<@NotNull Long> tenantTrainingCourseTypeIds;

    @Size(max = MAX_NAME_LENGTH) private String name;

    @NotNull private LocalDate startDate;

    @NotNull private LocalDate endDate;

    @NotBlank @Size(max = 512) private String location;

    private BigDecimal hours;

    @Size(max = 2048) private String notes;

    /** Proof documents; at least one is required for the workflow submission path. */
    @Size(max = MAX_ATTACHMENTS) private List<@NotNull Long> attachmentIds;
}
