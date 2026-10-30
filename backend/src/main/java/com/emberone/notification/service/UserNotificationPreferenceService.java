/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.service;

import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.domain.UserNotificationChannelOverride;
import com.emberone.notification.dto.NotificationPreferenceDto;
import com.emberone.notification.dto.NotificationPreferencesDto;
import com.emberone.notification.dto.UpdateNotificationPreferenceRequest;
import com.emberone.notification.repository.UserNotificationChannelOverrideRepository;
import com.emberone.notification.type.NotificationTypeDefinitionRegistry;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserNotificationPreferenceService {

    private final UserNotificationChannelOverrideRepository overrideRepository;
    private final UserRepository userRepository;
    private final NotificationTypeDefinitionRegistry typeDefinitions;

    private static Set<NotificationChannel> normalizeChannels(Set<NotificationChannel> channels) {
        if (channels == null || channels.isEmpty()) {
            return Set.of();
        }
        return EnumSet.copyOf(channels);
    }

    @Transactional(readOnly = true)
    public NotificationPreferencesDto getPreferences(Long userId) {
        ensureUserExists(userId);
        Map<NotificationType, List<UserNotificationChannelOverride>> overridesByType =
                overrideRepository.findAllByUserId(userId).stream()
                        .collect(
                                Collectors.groupingBy(
                                        UserNotificationChannelOverride::getNotificationType));

        List<NotificationPreferenceDto> preferences =
                Arrays.stream(NotificationType.values())
                        .map(type -> toDto(type, overridesByType.getOrDefault(type, List.of())))
                        .toList();
        return NotificationPreferencesDto.builder().preferences(preferences).build();
    }

    @Transactional
    public NotificationPreferenceDto updatePreference(
            Long userId, NotificationType type, UpdateNotificationPreferenceRequest request) {
        var user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        Set<NotificationChannel> requestedChannels = normalizeChannels(request.channels());
        Set<NotificationChannel> defaultChannels = defaultChannelsFor(type);

        for (NotificationChannel channel : NotificationChannel.values()) {
            boolean requested = requestedChannels.contains(channel);
            boolean defaultEnabled = defaultChannels.contains(channel);
            syncOverride(user.getTenantId(), userId, type, channel, requested, defaultEnabled);
        }

        List<UserNotificationChannelOverride> overrides =
                overrideRepository.findAllByUserIdAndNotificationType(userId, type);
        return toDto(type, overrides);
    }

    @Transactional(readOnly = true)
    public Set<NotificationChannel> resolveChannels(Long userId, NotificationType type) {
        Map<NotificationChannel, Boolean> overrides =
                overrideRepository.findAllByUserIdAndNotificationType(userId, type).stream()
                        .collect(
                                Collectors.toMap(
                                        UserNotificationChannelOverride::getChannel,
                                        UserNotificationChannelOverride::isEnabled));

        Set<NotificationChannel> defaultChannels = defaultChannelsFor(type);
        EnumSet<NotificationChannel> resolved = EnumSet.noneOf(NotificationChannel.class);
        for (NotificationChannel channel : NotificationChannel.values()) {
            Boolean override = overrides.get(channel);
            boolean enabled = override != null ? override : defaultChannels.contains(channel);
            if (enabled) {
                resolved.add(channel);
            }
        }
        return resolved;
    }

    private void syncOverride(
            Long tenantId,
            Long userId,
            NotificationType type,
            NotificationChannel channel,
            boolean requested,
            boolean defaultEnabled) {
        if (requested == defaultEnabled) {
            overrideRepository.deleteByUserIdAndNotificationTypeAndChannel(userId, type, channel);
            return;
        }

        UserNotificationChannelOverride override =
                overrideRepository
                        .findByUserIdAndNotificationTypeAndChannel(userId, type, channel)
                        .orElseGet(
                                () ->
                                        UserNotificationChannelOverride.builder()
                                                .tenantId(tenantId)
                                                .userId(userId)
                                                .notificationType(type)
                                                .channel(channel)
                                                .build());
        override.setEnabled(requested);
        overrideRepository.save(override);
    }

    private NotificationPreferenceDto toDto(
            NotificationType type, List<UserNotificationChannelOverride> overrides) {
        Set<NotificationChannel> defaultChannels = defaultChannelsFor(type);
        Map<NotificationChannel, Boolean> overrideByChannel =
                overrides.stream()
                        .collect(
                                Collectors.toMap(
                                        UserNotificationChannelOverride::getChannel,
                                        UserNotificationChannelOverride::isEnabled));

        EnumSet<NotificationChannel> effectiveChannels = EnumSet.noneOf(NotificationChannel.class);
        for (NotificationChannel channel : NotificationChannel.values()) {
            Boolean override = overrideByChannel.get(channel);
            boolean enabled = override != null ? override : defaultChannels.contains(channel);
            if (enabled) {
                effectiveChannels.add(channel);
            }
        }

        return NotificationPreferenceDto.builder()
                .type(type)
                .channels(effectiveChannels)
                .defaultChannels(defaultChannels)
                .customized(!overrides.isEmpty())
                .build();
    }

    private Set<NotificationChannel> defaultChannelsFor(NotificationType type) {
        return typeDefinitions.require(type).defaultChannels();
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
    }
}
