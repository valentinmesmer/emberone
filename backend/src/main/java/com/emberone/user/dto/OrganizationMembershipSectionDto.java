/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.dto;

import com.emberone.organization.dto.OrganizationDto;
import com.emberone.userorganizationmembership.dto.UserOrganizationMembershipDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMembershipSectionDto {
    private OrganizationDto organization;
    private boolean canWrite;

    /** True when the user currently has an active membership in this org's leadership group. */
    private boolean inLeadershipGroup;

    /**
     * True when the current user may open {@code ORGANIZATION_MEMBERSHIP_END_REQUEST} for this
     * member in this org (active membership, not leadership, workflow enabled, etc.).
     */
    private boolean canRequestOrganizationMembershipEnd;

    private boolean canRequestOrganizationUserMainOrganizationChange;

    private List<UserOrganizationMembershipDto> organizationMemberships;
    private List<GroupMembershipSectionDto> groups;
}
