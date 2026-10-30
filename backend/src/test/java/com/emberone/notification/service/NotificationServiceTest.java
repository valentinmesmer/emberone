/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.notification.domain.Notification;
import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationDeliveryStatus;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.event.NotificationCreatedEvent;
import com.emberone.notification.repository.NotificationRepository;
import com.emberone.platform.navigation.domain.AppLink;
import com.emberone.user.repository.UserRepository;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long TENANT_ID = 7L;

    @Mock NotificationRepository notificationRepository;
    @Mock UserRepository userRepository;
    @Mock UserNotificationPreferenceService preferenceService;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks NotificationService notificationService;

    @Test
    void notifyCreatesDeliveryPerChannelAndPublishesCreatedEvent() {
        when(userRepository.existsByIdAndTenantId(42L, TENANT_ID)).thenReturn(true);
        when(preferenceService.resolveChannels(42L, NotificationType.WORKFLOW_APPROVED))
                .thenReturn(Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL));
        when(notificationRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            Notification toSave = invocation.getArgument(0);
                            return toSave.toBuilder().id(1L).build();
                        });

        notificationService.notify(
                NotificationRequest.builder()
                        .tenantId(TENANT_ID)
                        .recipientUserIds(Set.of(42L))
                        .type(NotificationType.WORKFLOW_APPROVED)
                        .params(Map.of("workflowId", 5))
                        .link(AppLink.workflow(5))
                        .build());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getDeliveries()).hasSize(2);
        assertThat(saved.getDeliveries())
                .anySatisfy(
                        d -> {
                            assertThat(d.getChannel()).isEqualTo(NotificationChannel.IN_APP);
                            assertThat(d.getStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
                            assertThat(d.getSentAt()).isNotNull();
                        })
                .anySatisfy(
                        d -> {
                            assertThat(d.getChannel()).isEqualTo(NotificationChannel.EMAIL);
                            assertThat(d.getStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
                        });
        verify(eventPublisher).publishEvent(new NotificationCreatedEvent(1L));
    }

    @Test
    void notifySkipsUnknownRecipients() {
        when(userRepository.existsByIdAndTenantId(99L, TENANT_ID)).thenReturn(false);

        notificationService.notify(
                NotificationRequest.builder()
                        .tenantId(TENANT_ID)
                        .recipientUserIds(Set.of(99L))
                        .type(NotificationType.WORKFLOW_OPENED)
                        .build());

        verify(notificationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(preferenceService, never()).resolveChannels(any(), any());
    }

    @Test
    void notifyPublishesCreatedEventForEmailOnlyNotifications() {
        when(userRepository.existsByIdAndTenantId(42L, TENANT_ID)).thenReturn(true);
        when(preferenceService.resolveChannels(42L, NotificationType.USERNAME_CHANGED))
                .thenReturn(Set.of(NotificationChannel.EMAIL));
        when(notificationRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            Notification toSave = invocation.getArgument(0);
                            return toSave.toBuilder().id(2L).build();
                        });

        notificationService.notify(
                NotificationRequest.builder()
                        .tenantId(TENANT_ID)
                        .recipientUserIds(Set.of(42L))
                        .type(NotificationType.USERNAME_CHANGED)
                        .build());

        verify(eventPublisher).publishEvent(new NotificationCreatedEvent(2L));
    }

    @Test
    void notifySkipsRecipientWhenUserDisabledAllChannels() {
        when(userRepository.existsByIdAndTenantId(42L, TENANT_ID)).thenReturn(true);
        when(preferenceService.resolveChannels(42L, NotificationType.WORKFLOW_OPENED))
                .thenReturn(Set.of());

        notificationService.notify(
                NotificationRequest.builder()
                        .tenantId(TENANT_ID)
                        .recipientUserIds(Set.of(42L))
                        .type(NotificationType.WORKFLOW_OPENED)
                        .build());

        verify(notificationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
