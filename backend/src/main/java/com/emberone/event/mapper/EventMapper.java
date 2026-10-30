/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.mapper;

import com.emberone.event.domain.OperationEvent;
import com.emberone.event.domain.ServiceEvent;
import com.emberone.event.dto.OperationEventDto;
import com.emberone.event.dto.ServiceEventDto;
import com.emberone.event.dto.UpdateOperationEventRequest;
import com.emberone.event.dto.UpdateServiceEventRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EventMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "leaderId", source = "leader.id")
    @Mapping(target = "eventType", expression = "java(com.emberone.event.domain.EventType.SERVICE)")
    ServiceEventDto toDto(ServiceEvent event);

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "leaderId", source = "leader.id")
    @Mapping(
            target = "eventType",
            expression = "java(com.emberone.event.domain.EventType.OPERATION)")
    OperationEventDto toDto(OperationEvent event);

    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "leader", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    void updateFromRequest(UpdateServiceEventRequest request, @MappingTarget ServiceEvent event);

    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "leader", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    void updateFromRequest(
            UpdateOperationEventRequest request, @MappingTarget OperationEvent event);
}
