/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.mapper;

import com.emberone.event.domain.EventAttendance;
import com.emberone.event.domain.OperationEvent;
import com.emberone.event.domain.ServiceEvent;
import com.emberone.event.dto.EventAttendanceDto;
import com.emberone.event.dto.EventAttendancePageDto;
import com.emberone.event.dto.OperationEventPageDto;
import com.emberone.event.dto.ServiceEventPageDto;
import com.emberone.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventDtoAssembler {

    private final EventMapper eventMapper;

    public ServiceEventPageDto toServicePageDto(Page<ServiceEvent> page) {
        return ServiceEventPageDto.builder()
                .content(page.getContent().stream().map(eventMapper::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    public OperationEventPageDto toOperationPageDto(Page<OperationEvent> page) {
        return OperationEventPageDto.builder()
                .content(page.getContent().stream().map(eventMapper::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    public EventAttendancePageDto toAttendancePageDto(Page<EventAttendance> page) {
        return EventAttendancePageDto.builder()
                .content(page.getContent().stream().map(this::toAttendanceDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    private EventAttendanceDto toAttendanceDto(EventAttendance entity) {
        User u = entity.getUser();
        return EventAttendanceDto.builder()
                .id(entity.getId())
                .eventType(entity.getEventType())
                .eventId(entity.getEventId())
                .userId(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .build();
    }
}
