/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.event;

/**
 * Published after a notification has been persisted so realtime and future channel adapters can
 * subscribe without {@code NotificationService} depending on them directly.
 */
public record NotificationCreatedEvent(Long notificationId) {}
