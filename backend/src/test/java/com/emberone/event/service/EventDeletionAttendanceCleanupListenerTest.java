/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.event.service;

import static org.mockito.Mockito.verify;

import com.emberone.event.domain.EventType;
import com.emberone.event.event.EventDeletingEvent;
import com.emberone.event.repository.EventAttendanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventDeletionAttendanceCleanupListenerTest {

    @Mock EventAttendanceRepository eventAttendanceRepository;
    @InjectMocks EventDeletionAttendanceCleanupListener listener;

    @Test
    void onEventDeletingDelegatesToAttendanceRepository() {
        listener.onEventDeleting(new EventDeletingEvent(1L, EventType.SERVICE, 5L));

        verify(eventAttendanceRepository).deleteByEventTypeAndEventId(EventType.SERVICE, 5L);
    }
}
