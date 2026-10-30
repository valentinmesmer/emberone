/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.service;

import com.emberone.event.event.EventDeletingEvent;
import com.emberone.event.repository.EventAttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventDeletionAttendanceCleanupListener {

    private final EventAttendanceRepository eventAttendanceRepository;

    /**
     * Runs synchronously in the deleting transaction so attendance cleanup rolls back together with
     * the event delete if anything fails.
     */
    @EventListener
    public void onEventDeleting(EventDeletingEvent event) {
        eventAttendanceRepository.deleteByEventTypeAndEventId(event.eventType(), event.eventId());
    }
}
