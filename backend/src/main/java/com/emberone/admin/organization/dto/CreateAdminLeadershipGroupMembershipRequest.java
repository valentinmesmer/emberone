/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.organization.dto;

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
public class CreateAdminLeadershipGroupMembershipRequest {
    @NotNull private Long userId;
    @NotNull @OrganizationWallClock private Instant startDate;
    @OrganizationWallClock private Instant endDate;
    @NotNull private GroupMembershipType membershipType;
}
