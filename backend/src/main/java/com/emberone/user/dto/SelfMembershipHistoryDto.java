/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.user.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/** Current and past organization and group memberships of the current user. */
@Value
@Builder
public class SelfMembershipHistoryDto {
    List<OrganizationMembershipPeriodDto> organizations;
    List<GroupMembershipPeriodDto> groups;
}
