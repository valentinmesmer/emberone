/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import {
  type InfiniteData,
  type QueryClient,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { type NotificationListRequest, notificationsApi } from '@/lib/api/notifications';
import { isUnreadOnlyNotificationListQuery } from '@/lib/list/notificationList';
import type { ListQuery } from '@/lib/list/types';
import { useRealtimePollingFallback } from '@/lib/realtime/useRealtimePollingFallback';
import { usePagedList } from '@/lib/store/usePagedList';
import {
  isNotificationType,
  type NotificationDto,
  type NotificationPageDto,
  type UnreadNotificationCountDto,
} from '@/types/notification';

export const notificationKeys = {
  all: ['notifications'] as const,
  listRoot: () => [...notificationKeys.all, 'list'] as const,
  detail: (id: number) => [...notificationKeys.all, 'detail', id] as const,
  unreadCount: () => [...notificationKeys.all, 'unread-count'] as const,
};

function isNotificationListQueryKey(queryKey: readonly unknown[]): boolean {
  return queryKey[0] === notificationKeys.all[0] && queryKey[1] === 'list';
}

function listQueryFromKey(queryKey: readonly unknown[]): ListQuery | null {
  if (!isNotificationListQueryKey(queryKey)) {
    return null;
  }
  const listQuery = queryKey[2];
  if (typeof listQuery !== 'object' || listQuery === null) {
    return null;
  }
  return listQuery as ListQuery;
}

function isUnreadListQueryKey(queryKey: readonly unknown[]): boolean {
  const listQuery = listQueryFromKey(queryKey);
  if (!listQuery) {
    return false;
  }
  return isUnreadOnlyNotificationListQuery(listQuery);
}

function mapCachedPages(
  current: unknown,
  update: (page: NotificationPageDto, queryKey: readonly unknown[]) => NotificationPageDto,
  queryKey: readonly unknown[],
): unknown {
  if (current && typeof current === 'object' && 'pages' in current) {
    const infinite = current as InfiniteData<NotificationPageDto>;
    let changed = false;
    const pages = infinite.pages.map((page) => {
      const next = update(page, queryKey);
      if (next !== page) {
        changed = true;
      }
      return next;
    });
    return changed ? { ...infinite, pages } : current;
  }
  if (current && typeof current === 'object' && 'content' in current) {
    const next = update(current as NotificationPageDto, queryKey);
    return next === current ? current : next;
  }
  return current;
}

function forEachCachedNotificationList(
  queryClient: QueryClient,
  update: (current: NotificationPageDto, queryKey: readonly unknown[]) => NotificationPageDto,
): void {
  for (const query of queryClient.getQueryCache().findAll({
    queryKey: notificationKeys.all,
    exact: false,
  })) {
    if (!isNotificationListQueryKey(query.queryKey)) {
      continue;
    }
    const current = query.state.data;
    if (current == null) {
      continue;
    }
    const next = mapCachedPages(current, update, query.queryKey);
    if (next !== current) {
      queryClient.setQueryData(query.queryKey, next);
    }
  }
}

function applyMarkReadToList(
  current: NotificationPageDto,
  queryKey: readonly unknown[],
  updated: NotificationDto,
): NotificationPageDto {
  if (isUnreadListQueryKey(queryKey)) {
    if (!current.content.some((item) => item.id === updated.id)) {
      return current;
    }
    return {
      ...current,
      content: current.content.filter((item) => item.id !== updated.id),
      totalElements: Math.max(0, current.totalElements - 1),
    };
  }
  return {
    ...current,
    content: current.content.map((item) =>
      item.id === updated.id ? { ...item, ...updated, unread: false } : item,
    ),
  };
}

function decrementUnreadCount(queryClient: QueryClient): void {
  queryClient.setQueryData<UnreadNotificationCountDto>(
    notificationKeys.unreadCount(),
    (current) => ({
      count: Math.max(0, (current?.count ?? 0) - 1),
    }),
  );
}

function isNotificationDto(payload: unknown): payload is NotificationDto {
  const candidate = payload as NotificationDto;
  return (
    typeof payload === 'object' &&
    payload !== null &&
    typeof candidate.id === 'number' &&
    isNotificationType(candidate.type) &&
    typeof candidate.title === 'string' &&
    typeof candidate.body === 'string'
  );
}

function prependNotificationToCache(queryClient: QueryClient, notification: NotificationDto): void {
  forEachCachedNotificationList(queryClient, (current, queryKey) => {
    if (!isUnreadListQueryKey(queryKey)) {
      return current;
    }
    if (current.content.some((item) => item.id === notification.id)) {
      return current;
    }
    return {
      ...current,
      content: [notification, ...current.content],
      totalElements: current.totalElements + 1,
    };
  });
}

export function applyNotificationCreated(
  queryClient: QueryClient,
  payload: unknown,
): NotificationDto | null {
  if (!isNotificationDto(payload)) {
    return null;
  }
  prependNotificationToCache(queryClient, payload);
  void queryClient.invalidateQueries({ queryKey: notificationKeys.unreadCount() });
  return payload;
}

type UseNotificationListOptions = {
  listQuery: ListQuery;
  enabled?: boolean;
};

export function useNotification(notificationId: number | null | undefined) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: notificationKeys.detail(notificationId ?? 0),
    queryFn: () => notificationsApi.get(notificationId ?? 0),
    enabled: hasEmberOneUserId && notificationId != null && notificationId > 0,
    staleTime: 30 * 1000,
  });
}

export function useNotificationList({ listQuery, enabled = true }: UseNotificationListOptions) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return usePagedList<NotificationDto, NotificationListRequest>({
    queryKey: notificationKeys.listRoot(),
    listQuery: listQuery as NotificationListRequest,
    queryFn: (query) => notificationsApi.list(query),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 30 * 1000,
  });
}

/** First page only (e.g. notification panel). */
export function useNotificationListPage({ listQuery, enabled = true }: UseNotificationListOptions) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  const request = listQuery as NotificationListRequest;
  return useQuery({
    queryKey: [...notificationKeys.listRoot(), listQuery, 'page'] as const,
    queryFn: () => notificationsApi.list({ ...request, page: 0 }),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 30 * 1000,
  });
}

export function useUnreadNotificationCount(options?: { enabled?: boolean }) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  const refetchInterval = useRealtimePollingFallback(60 * 1000);
  return useQuery({
    queryKey: notificationKeys.unreadCount(),
    queryFn: () => notificationsApi.getUnreadCount(),
    enabled: hasEmberOneUserId && (options?.enabled ?? true),
    staleTime: 15 * 1000,
    refetchInterval,
  });
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => notificationsApi.markRead(id),
    onSuccess: (updated) => {
      let wasUnread = false;
      forEachCachedNotificationList(queryClient, (current, queryKey) => {
        const existing = current.content.find((item) => item.id === updated.id);
        if (existing?.unread) {
          wasUnread = true;
        }
        return applyMarkReadToList(current, queryKey, updated);
      });
      if (wasUnread) {
        decrementUnreadCount(queryClient);
      }
    },
  });
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => notificationsApi.markAllRead(),
    onSuccess: () => {
      const now = new Date().toISOString();
      forEachCachedNotificationList(queryClient, (current, queryKey) => {
        if (isUnreadListQueryKey(queryKey)) {
          return { ...current, content: [], totalElements: 0 };
        }
        return {
          ...current,
          content: current.content.map((item) => ({
            ...item,
            unread: false,
            readAt: item.readAt ?? now,
          })),
        };
      });
      queryClient.setQueryData<UnreadNotificationCountDto>(notificationKeys.unreadCount(), {
        count: 0,
      });
    },
  });
}
