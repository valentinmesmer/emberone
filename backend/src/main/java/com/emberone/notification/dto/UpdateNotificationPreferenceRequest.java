/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.notification.dto;

import com.emberone.notification.domain.NotificationChannel;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.Builder;

@Builder
public record UpdateNotificationPreferenceRequest(@NotNull Set<NotificationChannel> channels) {}
