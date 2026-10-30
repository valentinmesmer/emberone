/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
