/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.dto;

import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationType;
import java.util.Set;
import lombok.Builder;

@Builder
public record NotificationPreferenceDto(
        NotificationType type,
        Set<NotificationChannel> channels,
        Set<NotificationChannel> defaultChannels,
        boolean customized) {}
