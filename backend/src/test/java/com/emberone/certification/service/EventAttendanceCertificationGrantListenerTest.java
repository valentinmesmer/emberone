/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import static org.mockito.Mockito.verify;

import com.emberone.event.domain.EventType;
import com.emberone.event.event.EventAttendanceAddedEvent;
import com.emberone.event.event.EventAttendanceRemovedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventAttendanceCertificationGrantListenerTest {

    @Mock CertificationEventGrantService certificationEventGrantService;
    @InjectMocks EventAttendanceCertificationGrantListener listener;

    @Test
    void onAttendanceAddedDelegatesToGrantService() {
        listener.onAttendanceAdded(new EventAttendanceAddedEvent(1L, EventType.SERVICE, 5L, 7L));

        verify(certificationEventGrantService).onAttendanceAdded(EventType.SERVICE, 5L, 7L);
    }

    @Test
    void onAttendanceRemovedDelegatesToGrantService() {
        listener.onAttendanceRemoved(
                new EventAttendanceRemovedEvent(1L, EventType.OPERATION, 9L, 3L));

        verify(certificationEventGrantService).onAttendanceRemoved(EventType.OPERATION, 9L, 3L);
    }
}
