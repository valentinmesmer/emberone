/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.dto;

import com.emberone.appointment.domain.AppointmentResponseValue;
import com.emberone.platform.time.OrganizationWallClock;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyAppointmentResponseDto {
    private Long appointmentId;
    private AppointmentResponseValue value;
    private String comment;

    @OrganizationWallClock private Instant lastRespondedAt;
}
