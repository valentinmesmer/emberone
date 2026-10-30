/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationgroupmembership.mapper;

import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.dto.UpdateUserOrganizationGroupMembershipRequest;
import com.emberone.userorganizationgroupmembership.dto.UserOrganizationGroupMembershipDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserOrganizationGroupMembershipMapper {

    @Mapping(
            target = "userId",
            expression = "java(membership.getUser() != null ? membership.getUser().getId() : null)")
    @Mapping(
            target = "organizationGroupId",
            expression =
                    "java(membership.getOrganizationGroup() != null ? membership.getOrganizationGroup().getId() : null)")
    UserOrganizationGroupMembershipDto toDto(UserOrganizationGroupMembership membership);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "organizationGroup", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "endProcessedAt", ignore = true)
    void updateFromRequest(
            UpdateUserOrganizationGroupMembershipRequest request,
            @MappingTarget UserOrganizationGroupMembership membership);
}
