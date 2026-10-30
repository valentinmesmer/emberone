/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.realtime;

import static org.mockito.Mockito.*;

import com.emberone.notification.domain.Notification;
import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationDelivery;
import com.emberone.notification.domain.NotificationDeliveryStatus;
import com.emberone.notification.event.NotificationCreatedEvent;
import com.emberone.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationRealtimeListenerTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationRealtimeNotifier realtimeNotifier;

    @InjectMocks NotificationRealtimeListener listener;

    @Test
    void onNotificationCreatedPushesRealtimeForSentInAppDelivery() {
        Notification notification =
                Notification.builder()
                        .id(1L)
                        .tenantId(7L)
                        .recipientUserId(42L)
                        .deliveries(
                                List.of(
                                        NotificationDelivery.builder()
                                                .channel(NotificationChannel.IN_APP)
                                                .status(NotificationDeliveryStatus.SENT)
                                                .sentAt(Instant.now())
                                                .build(),
                                        NotificationDelivery.builder()
                                                .channel(NotificationChannel.EMAIL)
                                                .status(NotificationDeliveryStatus.PENDING)
                                                .build()))
                        .build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        listener.onNotificationCreated(new NotificationCreatedEvent(1L));

        verify(realtimeNotifier).notifyCreated(notification);
    }

    @Test
    void onNotificationCreatedSkipsRealtimeWhenInAppDeliveryMissing() {
        Notification notification =
                Notification.builder()
                        .id(2L)
                        .deliveries(
                                List.of(
                                        NotificationDelivery.builder()
                                                .channel(NotificationChannel.EMAIL)
                                                .status(NotificationDeliveryStatus.PENDING)
                                                .build()))
                        .build();
        when(notificationRepository.findById(2L)).thenReturn(Optional.of(notification));

        listener.onNotificationCreated(new NotificationCreatedEvent(2L));

        verify(realtimeNotifier, never()).notifyCreated(any());
    }
}
