/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { NotificationType } from '@/types/notification';
import type {
  NotificationPreferenceDto,
  NotificationPreferencesDto,
  UpdateNotificationPreferenceRequest,
} from '@/types/notificationPreference';
import { apiClient } from './client';

const BASE_PATH = '/api/v1/self/notification-preferences';

export const notificationPreferencesApi = {
  async getAll(): Promise<NotificationPreferencesDto> {
    return apiClient.get<NotificationPreferencesDto>(BASE_PATH);
  },

  async update(
    type: NotificationType,
    data: UpdateNotificationPreferenceRequest,
  ): Promise<NotificationPreferenceDto> {
    return apiClient.put<NotificationPreferenceDto>(`${BASE_PATH}/${type}`, data);
  },
};
