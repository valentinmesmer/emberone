/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.dto;

import com.emberone.platform.time.OrganizationWallClock;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Data;

@Data
public abstract class BaseUpdateEventRequest {

    @NotNull(message = "Start is required") @OrganizationWallClock
    private Instant start;

    @NotNull(message = "End is required") @OrganizationWallClock
    private Instant end;

    @NotBlank(message = "Location is required") private String location;

    private String description;

    @NotNull(message = "Leader ID is required") private Long leaderId;
}
