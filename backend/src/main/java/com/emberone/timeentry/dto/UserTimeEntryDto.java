/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.dto;

import com.emberone.organizationgroup.domain.OrganizationGroupType;
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
public class UserTimeEntryDto {
    private Long id;
    private Long userId;
    private Long organizationId;
    private String organizationName;
    private Long organizationGroupId;
    private String organizationGroupName;
    private OrganizationGroupType organizationGroupType;
    @OrganizationWallClock private Instant startTime;
    @OrganizationWallClock private Instant endTime;
    private String description;
}
