/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { NotificationChannel, NotificationType } from '@/types/notification';

export interface NotificationPreferenceDto {
  type: NotificationType;
  channels: NotificationChannel[];
  defaultChannels: NotificationChannel[];
  customized: boolean;
}

export interface NotificationPreferencesDto {
  preferences: NotificationPreferenceDto[];
}

export interface UpdateNotificationPreferenceRequest {
  channels: NotificationChannel[];
}
