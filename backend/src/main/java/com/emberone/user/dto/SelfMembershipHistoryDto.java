/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
