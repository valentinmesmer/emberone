/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.emberone.appointment.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class OrganizationAppointmentSelfControllerTest {

    @Mock AppointmentService appointmentService;

    @InjectMocks OrganizationAppointmentSelfController controller;

    @Test
    void markReadDelegates() {
        ResponseEntity<Void> response = controller.markRead(10L, 5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(appointmentService).markRead(10L, 5L);
    }

    @Test
    void markAllReadDelegates() {
        ResponseEntity<Void> response = controller.markAllRead(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(appointmentService).markAllReadForOrganization(10L);
    }
}
