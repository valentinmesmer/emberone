/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.appointment.dto.AppointmentDto;
import com.emberone.appointment.dto.MyAppointmentResponseDto;
import com.emberone.appointment.dto.UpcomingAppointmentsDto;
import com.emberone.appointment.service.AppointmentResponseService;
import com.emberone.appointment.service.AppointmentService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class SelfAppointmentControllerTest {

    @Mock AppointmentService appointmentService;
    @Mock AppointmentResponseService appointmentResponseService;

    @InjectMocks SelfAppointmentController controller;

    @Test
    void getUpcomingAppointmentsUsesDefaultLimit() {
        UpcomingAppointmentsDto page =
                UpcomingAppointmentsDto.builder()
                        .content(List.of(AppointmentDto.builder().id(1L).build()))
                        .totalCount(3)
                        .build();
        when(appointmentService.listUpcomingAcrossOrganizations(5)).thenReturn(page);

        ResponseEntity<UpcomingAppointmentsDto> response = controller.getUpcomingAppointments(5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(page);
        verify(appointmentService).listUpcomingAcrossOrganizations(5);
    }

    @Test
    void getMyResponsesDelegatesAppointmentIds() {
        List<Long> appointmentIds = List.of(1L, 2L);
        List<MyAppointmentResponseDto> responses =
                List.of(MyAppointmentResponseDto.builder().appointmentId(1L).build());
        when(appointmentResponseService.getMyResponses(appointmentIds)).thenReturn(responses);

        ResponseEntity<List<MyAppointmentResponseDto>> response =
                controller.getMyResponses(appointmentIds);

        assertThat(response.getBody()).isEqualTo(responses);
    }

    @Test
    void getMyUnreadCountsDelegates() {
        when(appointmentService.countUnreadAcrossOrganizations()).thenReturn(Map.of(10L, 2L));

        ResponseEntity<Map<Long, Long>> response = controller.getMyUnreadCounts();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of(10L, 2L));
    }
}
