/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import static org.mockito.Mockito.verify;

import com.emberone.event.domain.EventType;
import com.emberone.event.event.EventPromotedFromAppointmentEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventPromotionAppointmentListenerTest {

    @Mock AppointmentService appointmentService;
    @InjectMocks EventPromotionAppointmentListener listener;

    @Test
    void onEventPromotedFromAppointmentDelegatesToAppointmentService() {
        Instant start = LocalDateTime.of(2026, 3, 1, 10, 0).toInstant(ZoneOffset.UTC);
        Instant end = LocalDateTime.of(2026, 3, 1, 12, 0).toInstant(ZoneOffset.UTC);

        listener.onEventPromotedFromAppointment(
                new EventPromotedFromAppointmentEvent(10L, 42L, EventType.SERVICE, start, end));

        verify(appointmentService)
                .validateAndArchiveAfterEventPromote(10L, 42L, EventType.SERVICE, start, end);
    }
}
