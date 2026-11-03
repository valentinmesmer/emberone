/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.user.dto;

import com.emberone.organizationgroup.dto.OrganizationGroupDto;
import com.emberone.userorganizationgroupmembership.dto.UserOrganizationGroupMembershipDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMembershipSectionDto {
    private OrganizationGroupDto group;
    private List<UserOrganizationGroupMembershipDto> memberships;
}
