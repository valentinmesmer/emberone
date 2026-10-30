/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.mapper;

import com.emberone.organization.mapper.OrganizationMapper;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.dto.UserOrganizationMembershipDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        uses = {OrganizationMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserOrganizationMembershipMapper {

    @Mapping(
            target = "userId",
            expression = "java(membership.getUser() != null ? membership.getUser().getId() : null)")
    @Mapping(target = "organization", source = "organization")
    UserOrganizationMembershipDto toDto(UserOrganizationMembership membership);
}
