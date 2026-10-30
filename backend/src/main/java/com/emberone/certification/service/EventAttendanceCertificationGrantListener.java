/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import com.emberone.event.event.EventAttendanceAddedEvent;
import com.emberone.event.event.EventAttendanceRemovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventAttendanceCertificationGrantListener {

    private final CertificationEventGrantService certificationEventGrantService;

    /**
     * Runs synchronously in the attendance transaction so grant/record updates roll back together
     * with the attendance change if anything fails.
     */
    @EventListener
    public void onAttendanceAdded(EventAttendanceAddedEvent event) {
        certificationEventGrantService.onAttendanceAdded(
                event.eventType(), event.eventId(), event.userId());
    }

    @EventListener
    public void onAttendanceRemoved(EventAttendanceRemovedEvent event) {
        certificationEventGrantService.onAttendanceRemoved(
                event.eventType(), event.eventId(), event.userId());
    }
}
