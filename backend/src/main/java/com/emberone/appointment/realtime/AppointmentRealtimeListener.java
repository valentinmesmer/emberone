/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.realtime;

import com.emberone.appointment.domain.Appointment;
import com.emberone.appointment.event.AppointmentChangeType;
import com.emberone.appointment.event.AppointmentChangedEvent;
import com.emberone.appointment.exception.AppointmentNotFoundException;
import com.emberone.appointment.repository.AppointmentRepository;
import com.emberone.platform.event.AfterCommitEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppointmentRealtimeListener {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentRealtimeNotifier realtimeNotifier;

    @AfterCommitEventListener(readOnly = true)
    public void onAppointmentChanged(AppointmentChangedEvent event) {
        if (event.changeType() == AppointmentChangeType.DELETED) {
            realtimeNotifier.notifyUpdated(
                    event.tenantId(), event.organizationId(), event.appointmentId());
            return;
        }
        Appointment appointment =
                appointmentRepository
                        .findById(event.appointmentId())
                        .orElseThrow(() -> new AppointmentNotFoundException(event.appointmentId()));
        realtimeNotifier.notifyUpdated(appointment);
    }
}
