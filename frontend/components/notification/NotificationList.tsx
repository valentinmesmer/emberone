/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ListContainer } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import { notificationListUnreadQuery } from '@/lib/list/notificationList';
import type { ListQuery } from '@/lib/list/types';
import { useNotificationListPage } from '@/lib/store';
import type { NotificationDto } from '@/types/notification';
import { NotificationListItem } from './NotificationListItem';

interface NotificationListProps {
  onSelect: (notification: NotificationDto) => void;
  onOpenLink?: (notification: NotificationDto) => void;
  /** Defaults to unread inbox (notification panel). */
  listQuery?: ListQuery;
}

export function NotificationList({
  onSelect,
  onOpenLink,
  listQuery = notificationListUnreadQuery,
}: NotificationListProps) {
  const t = useTranslation();
  const { data, isPending, isError } = useNotificationListPage({ listQuery });

  if (isPending) {
    return (
      <p className="px-4 py-10 text-center text-sm text-[var(--text-secondary)]">
        {t('common.loading')}
      </p>
    );
  }

  if (isError) {
    return (
      <p className="px-4 py-10 text-center text-sm text-red-600 dark:text-red-400">
        {t('notifications.loadError')}
      </p>
    );
  }

  const items = data?.content ?? [];

  if (items.length === 0) {
    return (
      <p className="px-4 py-12 text-center text-sm text-[var(--text-secondary)]">
        {t('notifications.emptyUnread')}
      </p>
    );
  }

  return (
    <ListContainer label={t('notifications.title')} variant="flush">
      {items.map((notification) => (
        <NotificationListItem
          key={notification.id}
          notification={notification}
          onSelect={onSelect}
          onOpenLink={onOpenLink}
        />
      ))}
    </ListContainer>
  );
}
