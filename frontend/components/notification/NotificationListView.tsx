/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useRouter } from 'next/navigation';
import { useCallback, useMemo } from 'react';
import { ListContainer } from '@/components/ui/list';
import { ErrorState } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useTranslation } from '@/hooks/useTranslation';
import { useUrlSyncedListState } from '@/lib/list';
import {
  NotificationListStatusParam,
  notificationListDefinition,
} from '@/lib/list/notificationList';
import { getMyAreaNotificationDetailPath } from '@/lib/my-area/myAreaSections';
import {
  useMarkNotificationRead,
  useNotificationList,
  useUnreadNotificationCount,
} from '@/lib/store';
import type { NotificationDto } from '@/types/notification';
import { NotificationListItem } from './NotificationListItem';
import { NotificationMarkAllReadButton } from './NotificationMarkAllReadButton';

function selectedStatuses(raw: unknown): readonly string[] {
  return Array.isArray(raw)
    ? raw.filter((value): value is string => typeof value === 'string')
    : [];
}

export function NotificationListView() {
  const t = useTranslation();
  const router = useRouter();
  const listState = useUrlSyncedListState(notificationListDefinition);
  const statuses = selectedStatuses(listState.filters.status);
  const isReadOnly = statuses.length === 1 && statuses[0] === NotificationListStatusParam.READ;
  const isUnreadOnly = statuses.length === 1 && statuses[0] === NotificationListStatusParam.UNREAD;
  const isAllStatuses = statuses.length === 0;

  const {
    data: items,
    pages,
    isPending,
    isError,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useNotificationList({ listQuery: listState.listQuery });

  const { data: unreadCount } = useUnreadNotificationCount();
  const markRead = useMarkNotificationRead();

  const description = useMemo(() => {
    if (isAllStatuses) {
      const count = pages?.[0]?.totalElements ?? items.length;
      return count === 0
        ? t('myArea.notifications.allSubtitle')
        : t('myArea.notifications.allCount', { count });
    }
    if (isReadOnly) {
      const count = pages?.[0]?.totalElements ?? items.length;
      return count === 0
        ? t('myArea.notifications.readSubtitle')
        : t('myArea.notifications.readCount', { count });
    }
    const count = unreadCount?.count ?? pages?.[0]?.totalElements ?? items.length;
    if (count === 0) {
      return t('myArea.notifications.subtitle');
    }
    return t('myArea.notifications.unreadCount', { count });
  }, [isAllStatuses, isReadOnly, items.length, pages, t, unreadCount]);

  const handleMarkRead = useCallback(
    (notification: NotificationDto) => {
      if (notification.unread) {
        markRead.mutate(notification.id);
      }
    },
    [markRead],
  );

  const handleSelect = useCallback(
    (notification: NotificationDto) => {
      handleMarkRead(notification);
      router.push(getMyAreaNotificationDetailPath(notification.id));
    },
    [handleMarkRead, router],
  );

  const emptyMessage = isReadOnly
    ? t('notifications.emptyRead')
    : isAllStatuses
      ? t('notifications.emptyAll')
      : t('notifications.emptyUnread');

  return (
    <>
      <PageHeader
        title={t('myArea.notifications.title')}
        description={description}
        size="compact"
        action={
          isUnreadOnly ? (
            <NotificationMarkAllReadButton variant="outline" size="xs" fullWidth="responsive" />
          ) : null
        }
      />

      <PaginatedListView
        definition={notificationListDefinition}
        state={listState}
        totalItems={items.length}
        isPending={isPending}
        error={
          isError ? (
            <ErrorState message={t('notifications.loadError')} className="mt-1 sm:mt-0" />
          ) : undefined
        }
        emptyState={paginatedListEmptyCard(emptyMessage)}
        hasNextPage={!!hasNextPage}
        isFetchingNextPage={isFetchingNextPage}
        onLoadMore={() => void fetchNextPage()}
      >
        <ListContainer label={t('myArea.notifications.title')} variant="card" rowSeparator="divide">
          {items.map((notification) => (
            <NotificationListItem
              key={notification.id}
              notification={notification}
              onSelect={handleSelect}
              onOpenLink={handleMarkRead}
            />
          ))}
        </ListContainer>
      </PaginatedListView>
    </>
  );
}
