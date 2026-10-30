/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.mapper;

import com.emberone.timeentry.domain.UserTimeEntry;
import com.emberone.timeentry.dto.UserTimeEntryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserTimeEntryMapper {

    @Mapping(
            target = "userId",
            expression = "java(entity.getUser() != null ? entity.getUser().getId() : null)")
    @Mapping(
            target = "organizationId",
            expression =
                    "java(entity.getOrganization() != null ? entity.getOrganization().getId() : null)")
    @Mapping(
            target = "organizationName",
            expression =
                    "java(entity.getOrganization() != null ? entity.getOrganization().getName() : null)")
    @Mapping(
            target = "organizationGroupId",
            expression =
                    "java(entity.getOrganizationGroup() != null ? entity.getOrganizationGroup().getId() : null)")
    @Mapping(
            target = "organizationGroupName",
            expression =
                    "java(entity.getOrganizationGroup() != null ? entity.getOrganizationGroup().getName() : null)")
    @Mapping(
            target = "organizationGroupType",
            expression =
                    "java(entity.getOrganizationGroup() != null ? entity.getOrganizationGroup().getType() : null)")
    UserTimeEntryDto toDto(UserTimeEntry entity);
}
