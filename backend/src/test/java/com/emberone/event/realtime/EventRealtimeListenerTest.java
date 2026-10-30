/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.realtime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.event.domain.EventType;
import com.emberone.event.event.EventAttendanceAddedEvent;
import com.emberone.event.event.EventChangedEvent;
import com.emberone.event.event.EventDeletingEvent;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventRealtimeListenerTest {

    @Mock EventRealtimeNotifier notifier;
    @Mock EventOrganizationResolver organizationResolver;

    @InjectMocks EventRealtimeListener listener;

    @Test
    void onEventChangedNotifiesUpdated() {
        listener.onEventChanged(new EventChangedEvent(1L, 10L, EventType.SERVICE, 5L));

        verify(notifier).notifyUpdated(1L, 10L, EventType.SERVICE, 5L);
    }

    @Test
    void onAttendanceAddedResolvesOrganizationAndNotifies() {
        when(organizationResolver.resolveOrganizationId(EventType.OPERATION, 8L))
                .thenReturn(Optional.of(10L));

        listener.onAttendanceAdded(new EventAttendanceAddedEvent(1L, EventType.OPERATION, 8L, 42L));

        verify(notifier).notifyAttendanceChanged(1L, 10L, EventType.OPERATION, 8L);
    }

    @Test
    void onEventDeletingResolvesOrganizationAndNotifies() {
        when(organizationResolver.resolveOrganizationId(EventType.SERVICE, 5L))
                .thenReturn(Optional.of(10L));

        listener.onEventDeleting(new EventDeletingEvent(1L, EventType.SERVICE, 5L));

        verify(notifier).notifyDeleted(1L, 10L, EventType.SERVICE, 5L);
    }
}
