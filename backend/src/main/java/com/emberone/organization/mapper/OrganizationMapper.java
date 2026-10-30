/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.mapper;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.dto.OrganizationDto;
import com.emberone.organization.dto.UpdateOrganizationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OrganizationMapper {

    @Mapping(target = "parentOrganizationId", source = "parentOrganization.id")
    @Mapping(target = "organizationTypeId", source = "type.id")
    OrganizationDto toDto(Organization organization);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "parentOrganization", ignore = true)
    @Mapping(target = "standardId", ignore = true)
    @Mapping(target = "deactivatedAt", ignore = true)
    void updateFromRequest(
            UpdateOrganizationRequest request, @MappingTarget Organization organization);
}
