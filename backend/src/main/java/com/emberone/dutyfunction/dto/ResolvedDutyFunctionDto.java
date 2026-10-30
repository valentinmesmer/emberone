/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.dutyfunction.dto;

import com.emberone.organizationgroup.domain.GroupMembershipType;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ResolvedDutyFunctionDto {
    Long organizationId;
    String organizationName;
    String functionConfigKey;
    String functionName;
    String functionAbbreviation;
    String organizationGroupName;
    String organizationGroupConfigKey;
    GroupMembershipType membershipType;
    Instant membershipStartDate;
    Instant membershipEndDate;
    boolean active;
}
