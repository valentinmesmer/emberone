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
public class RemoveCourseRegistrationRequest {

    public static final int MAX_REASON_LENGTH = 2000;

    /** Why the organization ends the registration; shown to the user. */
    @NotBlank @Size(max = MAX_REASON_LENGTH) private String reason;
}
