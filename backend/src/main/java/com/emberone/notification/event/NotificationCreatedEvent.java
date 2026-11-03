/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.notification.event;

/**
 * Published after a notification has been persisted so realtime and future channel adapters can
 * subscribe without {@code NotificationService} depending on them directly.
 */
public record NotificationCreatedEvent(Long notificationId) {}
