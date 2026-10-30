/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.notification.dto.NotificationDto;
import com.emberone.notification.dto.NotificationPageDto;
import com.emberone.notification.dto.UnreadNotificationCountDto;
import com.emberone.notification.list.NotificationListFilter;
import com.emberone.notification.list.NotificationListStatus;
import com.emberone.notification.list.NotificationSelfListFilterOptions;
import com.emberone.notification.service.NotificationSelfService;
import com.emberone.platform.web.list.ListQuery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class NotificationSelfControllerTest {

    @Mock NotificationSelfService notificationSelfService;

    @Mock NotificationSelfListFilterOptions selfListFilterOptions;

    NotificationSelfController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationSelfController(notificationSelfService, selfListFilterOptions);
    }

    @Test
    void listNotificationsDelegatesToService() {
        NotificationPageDto page =
                NotificationPageDto.builder()
                        .content(List.of(NotificationDto.builder().id(1L).build()))
                        .build();
        ListQuery query = new ListQuery(1, 50, null, null);
        NotificationListFilter filter =
                new NotificationListFilter(List.of(NotificationListStatus.UNREAD), List.of());
        when(notificationSelfService.listNotifications(query, filter)).thenReturn(page);

        ResponseEntity<NotificationPageDto> response =
                controller.listNotifications(query, List.of("UNREAD"), List.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(page);
    }

    @Test
    void getNotificationDelegatesToService() {
        NotificationDto dto = NotificationDto.builder().id(9L).build();
        when(notificationSelfService.getNotification(9L)).thenReturn(dto);

        ResponseEntity<NotificationDto> response = controller.getNotification(9L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
        verify(notificationSelfService).getNotification(9L);
    }

    @Test
    void markReadDelegatesToService() {
        NotificationDto dto = NotificationDto.builder().id(5L).unread(false).build();
        when(notificationSelfService.markRead(5L)).thenReturn(dto);

        ResponseEntity<NotificationDto> response = controller.markRead(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
        verify(notificationSelfService).markRead(5L);
    }

    @Test
    void unreadCountDelegatesToService() {
        UnreadNotificationCountDto count = UnreadNotificationCountDto.builder().count(3).build();
        when(notificationSelfService.getUnreadCount()).thenReturn(count);

        ResponseEntity<UnreadNotificationCountDto> response = controller.getUnreadCount();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(count);
    }
}
