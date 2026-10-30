/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { createInitialListQueryState, defineList, toListQuery } from './defineList';
import type { ListQuery } from './types';

export const NotificationListStatusParam = {
  UNREAD: 'UNREAD',
  READ: 'READ',
} as const;

export type NotificationListStatusParam =
  (typeof NotificationListStatusParam)[keyof typeof NotificationListStatusParam];

const statusFilter = {
  kind: 'multiSelect',
  key: 'status',
  labelKey: 'notification.list.status.label',
  defaultValue: [NotificationListStatusParam.UNREAD],
  countsInMenubarWhenSelected: true,
  options: [
    { value: NotificationListStatusParam.UNREAD, labelKey: 'notification.list.status.unread' },
    { value: NotificationListStatusParam.READ, labelKey: 'notification.list.status.read' },
  ],
} as const;

const typeFilter = {
  kind: 'multiSelect',
  key: 'type',
  labelKey: 'notification.list.type.label',
  options: [],
  runtimeOptions: true,
} as const;

/**
 * Self `/my-area/notifications` - Status (default nur Ungelesen; keine Auswahl = alle) + Typ in menubar.
 */
export const notificationListDefinition = defineList({
  id: 'notification',
  filterOptionsSource: 'self/notifications',
  sheetLayout: 'menubar',
  filters: [statusFilter, typeFilter],
});

const initialNotificationListState = createInitialListQueryState(notificationListDefinition);

/** Default unread inbox (panel, badges). */
export const notificationListUnreadQuery: ListQuery = toListQuery(
  notificationListDefinition,
  initialNotificationListState,
);

export function isUnreadOnlyNotificationListQuery(listQuery: ListQuery): boolean {
  const status = listQuery.status;
  if (status === undefined) {
    return false;
  }
  if (typeof status === 'string') {
    return status === NotificationListStatusParam.UNREAD;
  }
  if (Array.isArray(status)) {
    return status.length === 1 && status[0] === NotificationListStatusParam.UNREAD;
  }
  return false;
}
