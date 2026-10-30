/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import static org.mockito.Mockito.verify;

import com.emberone.event.domain.EventType;
import com.emberone.event.event.EventDeletingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventDeletionCertificationGrantCleanupListenerTest {

    @Mock CertificationEventGrantService certificationEventGrantService;
    @InjectMocks EventDeletionCertificationGrantCleanupListener listener;

    @Test
    void onEventDeletingDelegatesToGrantService() {
        listener.onEventDeleting(new EventDeletingEvent(1L, EventType.SERVICE, 5L));

        verify(certificationEventGrantService).deleteGrantsForEvent(EventType.SERVICE, 5L);
    }
}
