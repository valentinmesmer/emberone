/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { ListQuery } from '@/lib/list/types';
import type {
  NotificationDto,
  NotificationPageDto,
  UnreadNotificationCountDto,
} from '@/types/notification';
import { apiClient } from './client';
import { buildQuery } from './paged';

const BASE_PATH = '/api/v1/self/notifications';

export type NotificationListRequest = ListQuery & {
  status?: readonly ('UNREAD' | 'READ')[];
  type?: readonly string[];
};

export const notificationsApi = {
  async list(query: NotificationListRequest): Promise<NotificationPageDto> {
    const qs = buildQuery({
      page: query.page ?? 0,
      size: query.size,
      search: query.search,
      sort: query.sort,
      status: query.status,
      type: query.type,
    });
    return apiClient.get<NotificationPageDto>(qs ? `${BASE_PATH}?${qs}` : BASE_PATH);
  },

  async get(id: number): Promise<NotificationDto> {
    return apiClient.get<NotificationDto>(`${BASE_PATH}/${id}`);
  },

  async getUnreadCount(): Promise<UnreadNotificationCountDto> {
    return apiClient.get<UnreadNotificationCountDto>(`${BASE_PATH}/unread-count`);
  },

  async markRead(id: number): Promise<NotificationDto> {
    return apiClient.post<NotificationDto>(`${BASE_PATH}/${id}/read`, {});
  },

  async markAllRead(): Promise<void> {
    return apiClient.post<void>(`${BASE_PATH}/read-all`, {});
  },
};
