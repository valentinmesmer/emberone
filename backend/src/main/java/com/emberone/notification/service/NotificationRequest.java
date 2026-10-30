/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.service;

import com.emberone.notification.domain.NotificationType;
import com.emberone.platform.navigation.domain.AppLink;
import java.util.Map;
import java.util.Set;
import lombok.Builder;

@Builder
public record NotificationRequest(
        Long tenantId,
        Set<Long> recipientUserIds,
        NotificationType type,
        Map<String, Object> params,
        AppLink link) {}
