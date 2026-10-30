/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** The quota of one existing audience entry, set together with the maximum number of seats. */
@Data
public class CourseAudienceQuotaRequest {

    @NotNull(message = "Audience entry is required") private Long audienceId;

    @NotNull(message = "Quota is required") @Min(value = 1, message = "Quota must be at least 1") private Integer quota;
}
