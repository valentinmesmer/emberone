/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { type InfiniteData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import {
  type UserDocumentListRequest,
  type UserDocumentPageDto,
  userDocumentApi,
} from '@/lib/api/userDocument';
import type { ListQuery } from '@/lib/list/types';
import { STALE_TIME } from '@/lib/store/staleTime';
import { usePagedList } from '@/lib/store/usePagedList';
import { getOrganizationWallClockNow } from '@/lib/utils/date';
import type { UnreadDocumentCountDto, UserDocumentDto } from '@/types/userDocument';

export const userDocumentKeys = {
  all: ['user-documents'] as const,
  listRoot: () => [...userDocumentKeys.all, 'list'] as const,
  byIds: (ids: readonly number[]) => [...userDocumentKeys.listRoot(), 'by-ids', ...ids] as const,
  detailRoot: () => [...userDocumentKeys.all, 'detail'] as const,
  detail: (documentId: number) => [...userDocumentKeys.detailRoot(), documentId] as const,
  tags: () => [...userDocumentKeys.all, 'tags'] as const,
  unreadCount: () => [...userDocumentKeys.all, 'unread-count'] as const,
};

type UseUserDocumentListOptions = {
  listQuery: ListQuery;
  enabled?: boolean;
};

export function useUserDocumentList({ listQuery, enabled = true }: UseUserDocumentListOptions) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return usePagedList<UserDocumentDto, UserDocumentListRequest>({
    queryKey: userDocumentKeys.listRoot(),
    listQuery: listQuery as UserDocumentListRequest,
    queryFn: (query) => userDocumentApi.list(query),
    enabled: hasEmberOneUserId && enabled,
    staleTime: STALE_TIME.LIST,
  });
}

export function useUserDocument(documentId: number, enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: userDocumentKeys.detail(documentId),
    queryFn: () => userDocumentApi.get(documentId),
    enabled: hasEmberOneUserId && enabled && documentId > 0,
    staleTime: STALE_TIME.LIST,
  });
}

/** Metadata of the documents linked to a record (ids come from the record's `userDocumentIds`). */
export function useUserDocumentsByIds(ids: readonly number[], enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: userDocumentKeys.byIds(ids),
    queryFn: () => userDocumentApi.listByIds(ids),
    enabled: hasEmberOneUserId && enabled && ids.length > 0,
    staleTime: STALE_TIME.LIST,
  });
}

export function useUserDocumentTags(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: userDocumentKeys.tags(),
    queryFn: () => userDocumentApi.listTags(),
    enabled: hasEmberOneUserId && enabled,
    staleTime: STALE_TIME.LIST,
  });
}

function forEachCachedDocumentList(
  queryClient: ReturnType<typeof useQueryClient>,
  update: (page: UserDocumentPageDto) => UserDocumentPageDto,
) {
  for (const query of queryClient.getQueryCache().findAll({
    queryKey: userDocumentKeys.all,
    exact: false,
  })) {
    const data = query.state.data;
    if (data && typeof data === 'object' && 'pages' in data) {
      const infinite = data as InfiniteData<UserDocumentPageDto>;
      queryClient.setQueryData(query.queryKey, {
        ...infinite,
        pages: infinite.pages.map(update),
      });
    }
  }
}

export function useUnreadDocumentCount(options?: { enabled?: boolean }) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: userDocumentKeys.unreadCount(),
    queryFn: () => userDocumentApi.getUnreadCount(),
    enabled: hasEmberOneUserId && (options?.enabled ?? true),
    staleTime: STALE_TIME.LIVE,
    refetchInterval: 120 * 1000,
  });
}

export function useMarkAllDocumentsViewed() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => userDocumentApi.markAllViewed(),
    onSuccess: () => {
      const now = getOrganizationWallClockNow();
      forEachCachedDocumentList(queryClient, (page) => ({
        ...page,
        content: page.content.map((document) => ({
          ...document,
          viewedAt: document.viewedAt ?? now,
        })),
      }));
      queryClient.setQueryData<UnreadDocumentCountDto>(userDocumentKeys.unreadCount(), {
        count: 0,
      });
      void queryClient.invalidateQueries({ queryKey: userDocumentKeys.listRoot() });
      void queryClient.invalidateQueries({ queryKey: userDocumentKeys.detailRoot() });
    },
  });
}

export function useMarkDocumentViewed() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (documentId: number) => userDocumentApi.markViewed(documentId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: userDocumentKeys.listRoot() });
      void queryClient.invalidateQueries({ queryKey: userDocumentKeys.detailRoot() });
      void queryClient.invalidateQueries({ queryKey: userDocumentKeys.unreadCount() });
    },
  });
}
