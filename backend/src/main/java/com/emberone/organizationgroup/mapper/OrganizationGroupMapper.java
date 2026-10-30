/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.mapper;

import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.dto.OrganizationGroupDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OrganizationGroupMapper {

    @Mapping(
            target = "organizationId",
            expression =
                    "java(group.getOrganization() != null ? group.getOrganization().getId() : null)")
    OrganizationGroupDto toDto(OrganizationGroup group);
}
