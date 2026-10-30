/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.dto;

import com.emberone.organizationgroup.domain.OrganizationGroupType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Organization group the current user may request to join, including its owning organization.
 * Leadership and all-members groups are never included; only custom groups the user is not already
 * a member of are join targets.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationGroupJoinTargetDto {

    private Long organizationId;
    private String organizationName;
    private Long organizationGroupId;
    private String organizationGroupName;
    private OrganizationGroupType organizationGroupType;
}
