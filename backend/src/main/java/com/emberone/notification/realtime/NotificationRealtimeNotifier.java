/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.realtime;

import com.emberone.notification.domain.Notification;
import com.emberone.notification.dto.NotificationDto;
import com.emberone.notification.mapper.NotificationDtoAssembler;
import com.emberone.platform.realtime.event.RealtimeEvent;
import com.emberone.platform.realtime.publish.RealtimePublisher;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationRealtimeNotifier {

    private final ObjectProvider<RealtimePublisher> realtimePublisherProvider;
    private final NotificationDtoAssembler notificationDtoAssembler;

    public void notifyCreated(Notification notification) {
        RealtimePublisher publisher = realtimePublisherProvider.getIfAvailable();
        if (publisher == null) {
            return;
        }
        NotificationDto dto = notificationDtoAssembler.toDto(notification);
        publisher.publishToUser(
                notification.getTenantId(),
                notification.getRecipientUserId(),
                RealtimePublisher.NOTIFICATIONS_QUEUE,
                RealtimeEvent.withPayload(
                        "notification.created", Map.of("id", notification.getId()), dto));
    }
}
