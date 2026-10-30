/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.realtime;

import static org.mockito.Mockito.verify;

import com.emberone.userorganizationmembership.event.OrganizationMembershipCreatedEvent;
import com.emberone.userorganizationmembership.event.OrganizationMembershipEndedEvent;
import com.emberone.userorganizationmembership.event.OrganizationMembershipUpdatedEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationMembershipRealtimeListenerTest {

    @Mock OrganizationMembershipRealtimeNotifier notifier;

    @InjectMocks OrganizationMembershipRealtimeListener listener;

    @Test
    void onCreatedNotifiesChanged() {
        listener.onCreated(new OrganizationMembershipCreatedEvent(1L, 42L, 10L));

        verify(notifier).notifyChanged(1L, 10L, 42L);
    }

    @Test
    void onUpdatedNotifiesChanged() {
        listener.onUpdated(new OrganizationMembershipUpdatedEvent(1L, 42L, 10L));

        verify(notifier).notifyChanged(1L, 10L, 42L);
    }

    @Test
    void onEndedNotifiesChanged() {
        listener.onEnded(
                new OrganizationMembershipEndedEvent(
                        1L, 42L, 10L, Instant.parse("2026-01-01T00:00:00Z")));

        verify(notifier).notifyChanged(1L, 10L, 42L);
    }
}
