/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
/** The current user's own response, embedded in {@link AppointmentDto#getMyResponse()}. */
public class MyAppointmentResponseDto {
    private AppointmentResponseValue value;
    private String comment;

    @OrganizationWallClock private Instant lastRespondedAt;
}
