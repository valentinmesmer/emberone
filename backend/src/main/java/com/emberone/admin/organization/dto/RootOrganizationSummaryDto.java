/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.organization.dto;

import com.emberone.user.dto.UserSummaryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RootOrganizationSummaryDto {
    private Long id;
    private String name;
    private String organizationCode;
    private Long organizationTypeId;
    private UserSummaryDto leader;
    private boolean hasChildren;

    /** True if this org or any descendant has no active member in the leadership group. */
    private boolean leaderMissingInSubtree;
}
