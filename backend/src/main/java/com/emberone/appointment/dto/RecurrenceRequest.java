/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.appointment.dto;

import com.emberone.platform.time.OrganizationWallClock;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Data;

@Data
public class RecurrenceRequest {
    @NotBlank(message = "Recurrence rule is required") @Size(max = 512, message = "Recurrence rule must be at most 512 characters") private String recurrenceRule;

    @OrganizationWallClock private Instant seriesEnd;

    @Size(max = 64, message = "Timezone must be at most 64 characters") private String timezone;
}
