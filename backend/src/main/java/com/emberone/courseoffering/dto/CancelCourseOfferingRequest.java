/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelCourseOfferingRequest {

    public static final int MAX_REASON_LENGTH = 2000;

    /** Why the organization cancels the offering; sent to everyone concerned. */
    @NotBlank @Size(max = MAX_REASON_LENGTH) private String reason;
}
