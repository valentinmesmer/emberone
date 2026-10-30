/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.notification.domain.*;
import com.emberone.notification.dto.NotificationDto;
import com.emberone.notification.dto.NotificationPageDto;
import com.emberone.notification.exception.NotificationNotFoundException;
import com.emberone.notification.list.NotificationListFilter;
import com.emberone.notification.list.NotificationListStatus;
import com.emberone.notification.mapper.NotificationDtoAssembler;
import com.emberone.notification.repository.NotificationRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class NotificationSelfServiceTest {

    private static final Long USER_ID = 42L;

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationDtoAssembler notificationDtoAssembler;
    @Mock TokenService tokenService;

    @InjectMocks NotificationSelfService notificationSelfService;

    private static Notification notificationWithChannel(NotificationChannel channel) {
        Notification notification =
                Notification.builder()
                        .id(1L)
                        .recipientUserId(USER_ID)
                        .type(NotificationType.WORKFLOW_OPENED)
                        .build();
        notification
                .getDeliveries()
                .add(
                        NotificationDelivery.builder()
                                .channel(channel)
                                .status(
                                        channel == NotificationChannel.IN_APP
                                                ? NotificationDeliveryStatus.SENT
                                                : NotificationDeliveryStatus.PENDING)
                                .build());
        return notification;
    }

    @Test
    void listNotificationsUsesSpecificationQuery() {
        when(tokenService.requireUserId()).thenReturn(USER_ID);
        Notification inApp = notificationWithChannel(NotificationChannel.IN_APP);
        Page<Notification> page = new PageImpl<>(List.of(inApp));
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(notificationDtoAssembler.toPageDto(page))
                .thenReturn(
                        NotificationPageDto.builder()
                                .content(List.of(NotificationDto.builder().id(1L).build()))
                                .build());

        assertThat(
                        notificationSelfService
                                .listNotifications(
                                        new ListQuery(0, null, null, null),
                                        new NotificationListFilter(
                                                List.of(NotificationListStatus.UNREAD), List.of()))
                                .getContent())
                .hasSize(1);
    }

    @Test
    void markReadRejectsForeignNotification() {
        when(tokenService.requireUserId()).thenReturn(USER_ID);
        when(notificationRepository.findByIdAndRecipientUserId(99L, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationSelfService.markRead(99L))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void markAllReadUpdatesOnlyCurrentUser() {
        when(tokenService.requireUserId()).thenReturn(USER_ID);

        notificationSelfService.markAllRead();

        verify(notificationRepository)
                .markAllReadForRecipient(
                        eq(USER_ID),
                        org.mockito.ArgumentMatchers.any(),
                        eq(NotificationChannel.IN_APP));
    }
}
