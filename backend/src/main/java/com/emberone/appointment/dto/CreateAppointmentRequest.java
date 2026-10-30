/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.dto;

import com.emberone.appointment.domain.AppointmentResponseMode;
import com.emberone.appointment.domain.AppointmentType;
import com.emberone.appointment.domain.AudienceScope;
import com.emberone.platform.time.OrganizationWallClock;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class CreateAppointmentRequest {
    @NotNull(message = "Organization ID is required") private Long organizationId;

    @NotBlank(message = "Name is required") @Size(max = 255, message = "Name must be at most 255 characters") private String name;

    @NotNull(message = "Type is required") private AppointmentType type;

    @NotNull(message = "Start is required") @OrganizationWallClock
    private Instant start;

    @NotNull(message = "End is required") @OrganizationWallClock
    private Instant end;

    @Size(max = 512, message = "Location must be at most 512 characters") private String location;

    private String description;

    @NotNull(message = "Response mode is required") private AppointmentResponseMode responseMode;

    private Integer responseDeadlineMinutesBefore;

    @NotNull(message = "Audience scope is required") private AudienceScope audienceScope;

    private List<Long> audienceGroupIds;

    @Valid private RecurrenceRequest recurrence;
}
