/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.service;

import com.emberone.notification.domain.Notification;
import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.dto.NotificationDto;
import com.emberone.notification.dto.NotificationPageDto;
import com.emberone.notification.dto.UnreadNotificationCountDto;
import com.emberone.notification.exception.NotificationNotFoundException;
import com.emberone.notification.list.NotificationListDefinition;
import com.emberone.notification.list.NotificationListFilter;
import com.emberone.notification.list.NotificationListSpecs;
import com.emberone.notification.mapper.NotificationDtoAssembler;
import com.emberone.notification.repository.NotificationRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSelfService {

    private final NotificationRepository notificationRepository;
    private final NotificationDtoAssembler notificationDtoAssembler;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public NotificationPageDto listNotifications(ListQuery query, NotificationListFilter filter) {
        Long userId = tokenService.requireUserId();
        Pageable pageable = NotificationListDefinition.INSTANCE.pageable(query);
        Specification<Notification> spec =
                Specification.allOf(
                        NotificationListSpecs.forRecipient(userId),
                        NotificationListSpecs.inAppChannel(),
                        NotificationListSpecs.statusesFilter(filter.statuses()),
                        NotificationListSpecs.typesFilter(filter.types()));
        Page<Notification> result = notificationRepository.findAll(spec, pageable);
        return notificationDtoAssembler.toPageDto(result);
    }

    @Transactional(readOnly = true)
    public NotificationDto getNotification(Long notificationId) {
        Long userId = tokenService.requireUserId();
        Specification<Notification> spec =
                Specification.allOf(
                        NotificationListSpecs.forRecipient(userId),
                        NotificationListSpecs.inAppChannel(),
                        (root, query, cb) -> cb.equal(root.get("id"), notificationId));
        Notification notification =
                notificationRepository
                        .findOne(spec)
                        .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        return notificationDtoAssembler.toDto(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationType> listDistinctTypesForUser() {
        Long userId = tokenService.requireUserId();
        return notificationRepository
                .findDistinctInAppTypesForRecipient(userId, NotificationChannel.IN_APP)
                .stream()
                .sorted(Comparator.comparing(Enum::name))
                .toList();
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountDto getUnreadCount() {
        Long userId = tokenService.requireUserId();
        long count =
                notificationRepository.countUnreadInAppForRecipient(
                        userId, NotificationChannel.IN_APP);
        return UnreadNotificationCountDto.builder().count(count).build();
    }

    @Transactional
    public NotificationDto markRead(Long notificationId) {
        Long userId = tokenService.requireUserId();
        Notification notification =
                notificationRepository
                        .findByIdAndRecipientUserId(notificationId, userId)
                        .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
        return notificationDtoAssembler.toDto(notification);
    }

    @Transactional
    public void markAllRead() {
        Long userId = tokenService.requireUserId();
        notificationRepository.markAllReadForRecipient(
                userId, Instant.now(), NotificationChannel.IN_APP);
    }
}
