/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.appointment.domain.SeriesEditScope;
import com.emberone.appointment.dto.AppointmentDto;
import com.emberone.appointment.dto.AppointmentPageDto;
import com.emberone.appointment.dto.CreateAppointmentRequest;
import com.emberone.appointment.list.AppointmentListFilter;
import com.emberone.appointment.list.AppointmentOrganizationListFilterOptions;
import com.emberone.appointment.service.AppointmentResponseService;
import com.emberone.appointment.service.AppointmentService;
import com.emberone.platform.web.list.ListQuery;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    @Mock AppointmentService appointmentService;
    @Mock AppointmentResponseService appointmentResponseService;
    @Mock AppointmentOrganizationListFilterOptions organizationListFilterOptions;

    @InjectMocks AppointmentController controller;

    @Test
    void listAppointmentsDelegatesToService() {
        AppointmentPageDto page =
                AppointmentPageDto.builder()
                        .content(List.of(AppointmentDto.builder().id(1L).build()))
                        .build();
        ListQuery query = new ListQuery(0, null, null, null);
        AppointmentListFilter filter =
                AppointmentListFilter.parseOrg(List.of("UPCOMING"), List.of(2026), null, null);
        when(appointmentService.listAppointments(20L, query, filter)).thenReturn(page);

        ResponseEntity<AppointmentPageDto> response =
                controller.listAppointments(
                        20L, query, List.of("UPCOMING"), List.of(2026), null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(page);
    }

    @Test
    void createAppointmentReturnsCreated() {
        CreateAppointmentRequest request = new CreateAppointmentRequest();
        AppointmentDto created = AppointmentDto.builder().id(1L).build();
        when(appointmentService.createAppointment(20L, request)).thenReturn(created);

        ResponseEntity<AppointmentDto> response = controller.createAppointment(20L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(created);
    }

    @Test
    void deleteAppointmentReturnsNoContent() {
        ResponseEntity<Void> response =
                controller.deleteAppointment(20L, 5L, SeriesEditScope.THIS_OCCURRENCE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(appointmentService).deleteAppointment(20L, 5L, SeriesEditScope.THIS_OCCURRENCE);
    }
}
