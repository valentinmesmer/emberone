/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { AppLinkFields } from '@/types/appLink';
import type { PageResponse } from '@/types/page';

export const NotificationType = {
  WORKFLOW_OPENED: 'WORKFLOW_OPENED',
  WORKFLOW_APPROVED: 'WORKFLOW_APPROVED',
  WORKFLOW_REJECTED: 'WORKFLOW_REJECTED',
  WORKFLOW_CANCELLED: 'WORKFLOW_CANCELLED',
  WORKFLOW_INPUT_CHANGE_REQUESTED: 'WORKFLOW_INPUT_CHANGE_REQUESTED',
  TODO_ASSIGNED: 'TODO_ASSIGNED',
  USERNAME_CHANGED: 'USERNAME_CHANGED',
  COURSE_REGISTRATION_REMOVED: 'COURSE_REGISTRATION_REMOVED',
  COURSE_OFFERING_CANCELLED: 'COURSE_OFFERING_CANCELLED',
  COURSE_OFFERING_SESSIONS_CHANGED: 'COURSE_OFFERING_SESSIONS_CHANGED',
  COURSE_OFFERING_START_REMINDER: 'COURSE_OFFERING_START_REMINDER',
} as const;

export type NotificationType = (typeof NotificationType)[keyof typeof NotificationType];

export const NotificationChannel = {
  IN_APP: 'IN_APP',
  EMAIL: 'EMAIL',
} as const;

export type NotificationChannel = (typeof NotificationChannel)[keyof typeof NotificationChannel];

export function isNotificationType(value: string): value is NotificationType {
  return Object.values(NotificationType).includes(value as NotificationType);
}

export interface NotificationDto extends AppLinkFields {
  id: number;
  type: NotificationType;
  title: string;
  body: string;
  params?: Record<string, unknown>;
  readAt?: string | null;
  unread: boolean;
}

export type NotificationPageDto = PageResponse<NotificationDto>;

export interface UnreadNotificationCountDto {
  count: number;
}
