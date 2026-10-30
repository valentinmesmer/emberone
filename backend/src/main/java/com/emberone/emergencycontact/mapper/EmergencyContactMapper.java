/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.emergencycontact.mapper;

import com.emberone.emergencycontact.domain.UserEmergencyContact;
import com.emberone.emergencycontact.dto.CreateEmergencyContactRequest;
import com.emberone.emergencycontact.dto.EmergencyContactDto;
import com.emberone.emergencycontact.dto.UpdateEmergencyContactRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EmergencyContactMapper {

    @Mapping(
            target = "userId",
            expression = "java(contact.getUser() != null ? contact.getUser().getId() : null)")
    EmergencyContactDto toDto(UserEmergencyContact contact);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "priority", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    UserEmergencyContact fromCreateRequest(CreateEmergencyContactRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "priority", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    void updateFromRequest(
            UpdateEmergencyContactRequest request, @MappingTarget UserEmergencyContact contact);
}
