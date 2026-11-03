/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
