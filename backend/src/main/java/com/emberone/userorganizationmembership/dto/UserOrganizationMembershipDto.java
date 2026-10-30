/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.dto;

import com.emberone.organization.dto.OrganizationDto;
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
public class UserOrganizationMembershipDto {
    private Long id;
    private Long userId;
    private Boolean main;
    @OrganizationWallClock private Instant startDate;
    @OrganizationWallClock private Instant endDate;
    private OrganizationDto organization;
}
