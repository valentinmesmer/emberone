/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect } from 'react';
import { type ToastType, useToast } from '@/components/ui/toast';
import { onNotificationCreated } from '@/lib/notification/realtimeToast';
import { useUIStore } from '@/lib/store';
import {
  NotificationType,
  type NotificationType as NotificationTypeValue,
} from '@/types/notification';

const TOAST_TYPE_BY_NOTIFICATION: Record<NotificationTypeValue, ToastType> = {
  [NotificationType.WORKFLOW_OPENED]: 'info',
  [NotificationType.WORKFLOW_APPROVED]: 'success',
  [NotificationType.WORKFLOW_REJECTED]: 'error',
  [NotificationType.WORKFLOW_CANCELLED]: 'warning',
  [NotificationType.WORKFLOW_INPUT_CHANGE_REQUESTED]: 'warning',
  [NotificationType.TODO_ASSIGNED]: 'info',
  [NotificationType.USERNAME_CHANGED]: 'info',
  [NotificationType.COURSE_REGISTRATION_REMOVED]: 'warning',
  [NotificationType.COURSE_OFFERING_CANCELLED]: 'warning',
  [NotificationType.COURSE_OFFERING_SESSIONS_CHANGED]: 'warning',
  [NotificationType.COURSE_OFFERING_START_REMINDER]: 'info',
};

/**
 * Shows a lightweight toast when a new notification arrives via realtime push.
 * Skips the toast while the notification panel is open.
 */
export function NotificationRealtimeToast() {
  const { showToast } = useToast();
  const panelOpen = useUIStore((s) => s.notificationPanelOpen);

  useEffect(() => {
    return onNotificationCreated((notification) => {
      if (panelOpen || !notification.unread) {
        return;
      }
      showToast(notification.title, TOAST_TYPE_BY_NOTIFICATION[notification.type]);
    });
  }, [panelOpen, showToast]);

  return null;
}
