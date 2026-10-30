/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { type InfiniteData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { fetchAllPageContent } from '@/lib/api/paged';
import {
  type UserDocumentListRequest,
  type UserDocumentPageDto,
  userDocumentApi,
} from '@/lib/api/userDocument';
import type { ListQuery } from '@/lib/list/types';
import { usePagedList } from '@/lib/store/usePagedList';
import { getOrganizationWallClockNow } from '@/lib/utils/date';
import type { UnreadDocumentCountDto, UserDocumentDto } from '@/types/userDocument';

export const userDocumentKeys = {
  all: ['user-documents'] as const,
  listRoot: () => [...userDocumentKeys.all, 'list'] as const,
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
    staleTime: 60 * 1000,
  });
}

/** Loads all pages (document pickers, detail lookups). */
export function useUserDocuments(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: [...userDocumentKeys.listRoot(), 'all'] as const,
    queryFn: () => fetchAllPageContent((page) => userDocumentApi.list({ page })),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 60 * 1000,
  });
}

export function useUserDocumentTags(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: userDocumentKeys.tags(),
    queryFn: () => userDocumentApi.listTags(),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 60 * 1000,
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
    staleTime: 30 * 1000,
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
    },
  });
}

export function useMarkDocumentViewed() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (documentId: number) => userDocumentApi.markViewed(documentId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: userDocumentKeys.listRoot() });
      void queryClient.invalidateQueries({ queryKey: userDocumentKeys.unreadCount() });
    },
  });
}
