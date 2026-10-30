/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import com.emberone.courseoffering.domain.CourseSessionType;
import com.emberone.platform.time.OrganizationWallClock;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Data;

@Data
public class CourseSessionRequest {

    @NotNull(message = "Type is required") private CourseSessionType type;

    @NotNull(message = "Start is required") @OrganizationWallClock
    private Instant start;

    @NotNull(message = "End is required") @OrganizationWallClock
    private Instant end;

    @Size(max = 512, message = "Location must be at most 512 characters") private String location;

    private String notes;
}
