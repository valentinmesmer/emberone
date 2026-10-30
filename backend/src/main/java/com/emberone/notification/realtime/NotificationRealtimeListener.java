/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.realtime;

import com.emberone.notification.domain.Notification;
import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationDeliveryStatus;
import com.emberone.notification.event.NotificationCreatedEvent;
import com.emberone.notification.exception.NotificationNotFoundException;
import com.emberone.notification.repository.NotificationRepository;
import com.emberone.platform.event.AfterCommitEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationRealtimeListener {

    private final NotificationRepository notificationRepository;
    private final NotificationRealtimeNotifier realtimeNotifier;

    private static boolean hasSentInAppDelivery(Notification notification) {
        return notification.getDeliveries().stream()
                .anyMatch(
                        delivery ->
                                delivery.getChannel() == NotificationChannel.IN_APP
                                        && delivery.getStatus() == NotificationDeliveryStatus.SENT);
    }

    @AfterCommitEventListener(readOnly = true)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        Notification notification = requireNotification(event.notificationId());
        if (!hasSentInAppDelivery(notification)) {
            return;
        }
        realtimeNotifier.notifyCreated(notification);
    }

    private Notification requireNotification(Long notificationId) {
        return notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
    }
}
