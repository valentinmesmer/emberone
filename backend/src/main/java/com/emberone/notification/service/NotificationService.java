/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.service;

import com.emberone.notification.domain.Notification;
import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationDelivery;
import com.emberone.notification.domain.NotificationDeliveryStatus;
import com.emberone.notification.event.NotificationCreatedEvent;
import com.emberone.notification.repository.NotificationRepository;
import com.emberone.platform.navigation.AppLinkSupport;
import com.emberone.platform.navigation.domain.AppLink;
import com.emberone.user.repository.UserRepository;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserNotificationPreferenceService preferenceService;
    private final ApplicationEventPublisher eventPublisher;

    private static void applyLink(Notification notification, AppLink link) {
        AppLinkSupport.applyTo(notification, link);
    }

    @Transactional
    public void notify(NotificationRequest request) {
        if (request == null || request.tenantId() == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (CollectionUtils.isEmpty(request.recipientUserIds())) {
            return;
        }

        Long tenantId = request.tenantId();
        for (Long recipientUserId : request.recipientUserIds()) {
            if (recipientUserId == null) {
                continue;
            }
            if (!userRepository.existsByIdAndTenantId(recipientUserId, tenantId)) {
                log.warn("Skipping notification for unknown user {}", recipientUserId);
                continue;
            }
            Set<NotificationChannel> channels =
                    preferenceService.resolveChannels(recipientUserId, request.type());
            if (channels.isEmpty()) {
                continue;
            }
            Notification notification =
                    createNotification(request, tenantId, recipientUserId, channels);
            notification = notificationRepository.save(notification);
            eventPublisher.publishEvent(new NotificationCreatedEvent(notification.getId()));
        }
    }

    private Notification createNotification(
            NotificationRequest request,
            Long tenantId,
            Long recipientUserId,
            Set<NotificationChannel> channels) {
        Instant now = Instant.now();
        Notification notification =
                Notification.builder()
                        .tenantId(tenantId)
                        .recipientUserId(recipientUserId)
                        .type(request.type())
                        .params(request.params())
                        .createdAt(now)
                        .build();
        applyLink(notification, request.link());

        for (NotificationChannel channel : channels) {
            NotificationDelivery delivery =
                    NotificationDelivery.builder()
                            .tenantId(tenantId)
                            .notification(notification)
                            .channel(channel)
                            .build();
            if (channel == NotificationChannel.IN_APP) {
                delivery.setStatus(NotificationDeliveryStatus.SENT);
                delivery.setSentAt(now);
            } else {
                delivery.setStatus(NotificationDeliveryStatus.PENDING);
            }
            notification.getDeliveries().add(delivery);
        }
        return notification;
    }
}
