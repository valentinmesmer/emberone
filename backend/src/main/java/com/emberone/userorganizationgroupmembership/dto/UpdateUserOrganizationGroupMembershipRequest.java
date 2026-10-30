/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationgroupmembership.dto;

import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.platform.time.OrganizationWallClock;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserOrganizationGroupMembershipRequest {

    @NotNull(message = "Start date is required") @OrganizationWallClock
    private Instant startDate;

    @OrganizationWallClock private Instant endDate;

    @NotNull(message = "Membership type is required") private GroupMembershipType membershipType;
}
