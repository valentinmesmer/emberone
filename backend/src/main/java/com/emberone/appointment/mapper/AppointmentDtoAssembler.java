/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.mapper;

import com.emberone.appointment.domain.Appointment;
import com.emberone.appointment.dto.AppointmentDto;
import com.emberone.appointment.dto.AppointmentPageDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentDtoAssembler {

    private final AppointmentMapper appointmentMapper;

    public AppointmentPageDto toPageDto(Page<Appointment> page, List<AppointmentDto> content) {
        return AppointmentPageDto.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    public List<AppointmentDto> toDtos(List<Appointment> appointments) {
        return appointments.stream().map(appointmentMapper::toDto).toList();
    }
}
