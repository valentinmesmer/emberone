/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.user.dto;

import com.emberone.organizationgroup.domain.GroupMembershipType;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

/** One group membership interval of the current user. */
@Value
@Builder
public class GroupMembershipPeriodDto {
    Long organizationId;
    Long organizationGroupId;
    String organizationGroupName;
    GroupMembershipType membershipType;
    Instant startDate;
    Instant endDate;
    boolean active;
}
