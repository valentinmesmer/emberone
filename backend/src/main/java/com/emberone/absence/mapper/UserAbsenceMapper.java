/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.mapper;

import com.emberone.absence.domain.UserAbsence;
import com.emberone.absence.dto.CreateUserAbsenceRequest;
import com.emberone.absence.dto.UpdateUserAbsenceRequest;
import com.emberone.absence.dto.UserAbsenceDto;
import java.util.List;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserAbsenceMapper {

    @Mapping(
            target = "userId",
            expression = "java(entity.getUser() != null ? entity.getUser().getId() : null)")
    @Mapping(target = "organizationIds", ignore = true)
    UserAbsenceDto toDto(UserAbsence entity);

    @AfterMapping
    default void fillOrganizationIds(UserAbsence entity, @MappingTarget UserAbsenceDto dto) {
        if (entity.getOrganizationLinks() == null || entity.getOrganizationLinks().isEmpty()) {
            dto.setOrganizationIds(List.of());
            return;
        }
        dto.setOrganizationIds(
                entity.getOrganizationLinks().stream()
                        .map(l -> l.getOrganization().getId())
                        .sorted()
                        .toList());
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "organizationLinks", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    void applyCreate(CreateUserAbsenceRequest request, @MappingTarget UserAbsence entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "organizationLinks", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    void updateFromRequest(UpdateUserAbsenceRequest request, @MappingTarget UserAbsence entity);
}
