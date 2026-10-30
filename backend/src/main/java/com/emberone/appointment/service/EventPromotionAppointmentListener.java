/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import com.emberone.event.event.EventPromotedFromAppointmentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventPromotionAppointmentListener {

    private final AppointmentService appointmentService;

    /**
     * Runs synchronously in the creating transaction so a failed promote validation rolls back the
     * newly created event.
     */
    @EventListener
    public void onEventPromotedFromAppointment(EventPromotedFromAppointmentEvent event) {
        appointmentService.validateAndArchiveAfterEventPromote(
                event.organizationId(),
                event.appointmentId(),
                event.eventType(),
                event.eventStart(),
                event.eventEnd());
    }
}
