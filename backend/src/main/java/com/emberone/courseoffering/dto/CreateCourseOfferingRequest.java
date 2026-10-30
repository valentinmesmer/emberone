/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** The training course type is fixed at creation. */
@Data
@EqualsAndHashCode(callSuper = true)
public class CreateCourseOfferingRequest extends UpdateCourseOfferingRequest {

    @NotNull(message = "Training course type is required") private Long trainingCourseTypeId;
}
