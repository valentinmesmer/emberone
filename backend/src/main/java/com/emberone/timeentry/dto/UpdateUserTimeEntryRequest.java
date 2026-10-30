/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.dto;

import com.emberone.platform.time.OrganizationWallClock;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserTimeEntryRequest {

    @NotNull(message = "Organization is required") private Long organizationId;

    private Long organizationGroupId;

    @NotNull(message = "Start time is required") @OrganizationWallClock
    private Instant startTime;

    @OrganizationWallClock private Instant endTime;

    @Size(max = 2000, message = "Description must be at most 2000 characters") private String description;
}
