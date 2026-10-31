/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.rank.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrganizationRankMemberDto {
    Long userId;
    String firstName;
    String lastName;
    boolean mainOrganization;
    UserRankSummaryDto currentRank;
}
