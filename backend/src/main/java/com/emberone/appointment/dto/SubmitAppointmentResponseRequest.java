/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.appointment.dto;

import com.emberone.appointment.domain.AppointmentResponseValue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitAppointmentResponseRequest {
    @NotNull(message = "Response value is required") private AppointmentResponseValue value;

    private String comment;
}
