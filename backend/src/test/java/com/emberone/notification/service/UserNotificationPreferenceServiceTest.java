/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.domain.UserNotificationChannelOverride;
import com.emberone.notification.dto.UpdateNotificationPreferenceRequest;
import com.emberone.notification.repository.UserNotificationChannelOverrideRepository;
import com.emberone.notification.type.NotificationTypeDefinition;
import com.emberone.notification.type.NotificationTypeDefinitionRegistry;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserNotificationPreferenceServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long TENANT_ID = 7L;

    @Mock UserNotificationChannelOverrideRepository overrideRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationTypeDefinitionRegistry typeDefinitions;

    @InjectMocks UserNotificationPreferenceService preferenceService;

    @BeforeEach
    void stubTypeDefaults() {
        when(typeDefinitions.require(any(NotificationType.class)))
                .thenAnswer(
                        invocation -> {
                            NotificationType type = invocation.getArgument(0);
                            NotificationTypeDefinition definition =
                                    org.mockito.Mockito.mock(NotificationTypeDefinition.class);
                            Set<NotificationChannel> channels =
                                    type == NotificationType.USERNAME_CHANGED
                                            ? Set.of(NotificationChannel.EMAIL)
                                            : Set.of(
                                                    NotificationChannel.IN_APP,
                                                    NotificationChannel.EMAIL);
                            when(definition.defaultChannels()).thenReturn(channels);
                            return definition;
                        });
    }

    @Test
    void resolveChannelsUsesDefaultsWhenNoOverridesStored() {
        when(overrideRepository.findAllByUserIdAndNotificationType(
                        USER_ID, NotificationType.USERNAME_CHANGED))
                .thenReturn(List.of());

        Set<NotificationChannel> channels =
                preferenceService.resolveChannels(USER_ID, NotificationType.USERNAME_CHANGED);

        assertThat(channels).containsExactly(NotificationChannel.EMAIL);
    }

    @Test
    void resolveChannelsUsesSparseOverrideForSingleChannel() {
        when(overrideRepository.findAllByUserIdAndNotificationType(
                        USER_ID, NotificationType.TODO_ASSIGNED))
                .thenReturn(
                        List.of(
                                UserNotificationChannelOverride.builder()
                                        .userId(USER_ID)
                                        .tenantId(TENANT_ID)
                                        .notificationType(NotificationType.TODO_ASSIGNED)
                                        .channel(NotificationChannel.EMAIL)
                                        .enabled(false)
                                        .build()));

        Set<NotificationChannel> channels =
                preferenceService.resolveChannels(USER_ID, NotificationType.TODO_ASSIGNED);

        assertThat(channels).containsExactly(NotificationChannel.IN_APP);
    }

    @Test
    void updatePreferenceStoresOnlyDeviationsFromDefaults() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(User.builder().id(USER_ID).tenantId(TENANT_ID).build()));
        when(overrideRepository.findByUserIdAndNotificationTypeAndChannel(
                        USER_ID, NotificationType.WORKFLOW_APPROVED, NotificationChannel.IN_APP))
                .thenReturn(Optional.empty());
        when(overrideRepository.findAllByUserIdAndNotificationType(
                        USER_ID, NotificationType.WORKFLOW_APPROVED))
                .thenReturn(
                        List.of(
                                UserNotificationChannelOverride.builder()
                                        .notificationType(NotificationType.WORKFLOW_APPROVED)
                                        .channel(NotificationChannel.IN_APP)
                                        .enabled(false)
                                        .build()));

        var result =
                preferenceService.updatePreference(
                        USER_ID,
                        NotificationType.WORKFLOW_APPROVED,
                        UpdateNotificationPreferenceRequest.builder()
                                .channels(Set.of(NotificationChannel.EMAIL))
                                .build());

        assertThat(result.channels()).containsExactly(NotificationChannel.EMAIL);
        assertThat(result.customized()).isTrue();

        ArgumentCaptor<UserNotificationChannelOverride> captor =
                ArgumentCaptor.forClass(UserNotificationChannelOverride.class);
        verify(overrideRepository).save(captor.capture());
        assertThat(captor.getValue().getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(captor.getValue().isEnabled()).isFalse();
        verify(overrideRepository)
                .deleteByUserIdAndNotificationTypeAndChannel(
                        USER_ID, NotificationType.WORKFLOW_APPROVED, NotificationChannel.EMAIL);
    }

    @Test
    void updatePreferenceRemovesOverridesWhenMatchingDefaults() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(User.builder().id(USER_ID).tenantId(TENANT_ID).build()));
        when(overrideRepository.findAllByUserIdAndNotificationType(
                        USER_ID, NotificationType.USERNAME_CHANGED))
                .thenReturn(List.of());

        var result =
                preferenceService.updatePreference(
                        USER_ID,
                        NotificationType.USERNAME_CHANGED,
                        UpdateNotificationPreferenceRequest.builder()
                                .channels(Set.of(NotificationChannel.EMAIL))
                                .build());

        assertThat(result.customized()).isFalse();
        verify(overrideRepository)
                .deleteByUserIdAndNotificationTypeAndChannel(
                        eq(USER_ID),
                        eq(NotificationType.USERNAME_CHANGED),
                        eq(NotificationChannel.EMAIL));
        verify(overrideRepository, never()).save(any());
    }
}
