/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.dutyfunction.dto;

import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.rank.dto.UserRankSummaryDto;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrganizationFunctionOverviewMemberDto {
    Long userId;
    String firstName;
    String lastName;
    boolean mainOrganization;
    GroupMembershipType membershipType;
    UserRankSummaryDto currentRank;
}
