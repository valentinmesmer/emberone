/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

@Data
public class InstantiateCourseOfferingTemplateRequest {

    /** Day of the first session of the new offering (organization calendar). */
    @NotNull(message = "First session date is required") private LocalDate firstSessionDate;
}
