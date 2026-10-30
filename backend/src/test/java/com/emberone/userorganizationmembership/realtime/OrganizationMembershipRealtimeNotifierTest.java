/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emberone.platform.realtime.event.RealtimeEvent;
import com.emberone.platform.realtime.publish.RealtimePublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class OrganizationMembershipRealtimeNotifierTest {

    @Mock ObjectProvider<RealtimePublisher> realtimePublisherProvider;
    @Mock RealtimePublisher publisher;

    @InjectMocks OrganizationMembershipRealtimeNotifier notifier;

    @Test
    void notifyChangedPublishesThinListTopic() {
        when(realtimePublisherProvider.getIfAvailable()).thenReturn(publisher);

        notifier.notifyChanged(7L, 10L, 42L);

        ArgumentCaptor<RealtimeEvent> eventCaptor = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(publisher)
                .publishToTopic(
                        eq(7L),
                        eq(OrganizationMembershipRealtimeNotifier.LIST_DOMAIN),
                        eventCaptor.capture());

        RealtimeEvent event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo(OrganizationMembershipRealtimeNotifier.EVENT_CHANGED);
        assertThat(event.payload()).isNull();
        assertThat(event.entityIds()).containsEntry("organizationId", 10L);
        assertThat(event.entityIds()).containsEntry("userId", 42L);
    }

    @Test
    void notifyChangedNoopsWhenPublisherUnavailable() {
        when(realtimePublisherProvider.getIfAvailable()).thenReturn(null);

        notifier.notifyChanged(7L, 10L, 42L);

        verifyNoInteractions(publisher);
    }
}
