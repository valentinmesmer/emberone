/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { type QueryKey, useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { fetchAllPageContent } from '@/lib/api/paged';
import type { ListQuery } from '@/lib/list/types';
import type { PageResponse } from '@/types/page';

export function nextPageParamFromResponse<T>(lastPage: PageResponse<T>): number | undefined {
  return lastPage.hasNext ? lastPage.page + 1 : undefined;
}

export function flatMapPageContent<T>(pages: PageResponse<T>[] | undefined): T[] {
  return pages?.flatMap((page) => page.content) ?? [];
}

type UsePagedListOptions<T, Q extends ListQuery> = {
  queryKey: QueryKey;
  /** Filter/sort/search state (without `page`); included in the React Query cache key. */
  listQuery: Q;
  queryFn: (query: Q & { page: number }) => Promise<PageResponse<T>>;
  enabled?: boolean;
  staleTime?: number;
  /** Loads every page in one query (for detail lookups). */
  fetchAll?: boolean;
};

export function usePagedList<T, Q extends ListQuery>({
  queryKey,
  listQuery,
  queryFn,
  enabled = true,
  staleTime,
  fetchAll = false,
}: UsePagedListOptions<T, Q>) {
  const resolvedQueryKey = useMemo(
    () => [...(Array.isArray(queryKey) ? queryKey : [queryKey]), listQuery] as const,
    [queryKey, listQuery],
  );

  const infinite = useInfiniteQuery({
    queryKey: resolvedQueryKey,
    queryFn: ({ pageParam }) => queryFn({ ...listQuery, page: pageParam }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => nextPageParamFromResponse(lastPage),
    enabled: enabled && !fetchAll,
    staleTime,
  });

  const allQuery = useQuery({
    queryKey: [...resolvedQueryKey, 'all'] as const,
    queryFn: () => fetchAllPageContent((page) => queryFn({ ...listQuery, page })),
    enabled: enabled && fetchAll,
    staleTime,
  });

  const pagedData = useMemo(() => flatMapPageContent(infinite.data?.pages), [infinite.data?.pages]);

  const fetchNextPage = fetchAll
    ? ((() => Promise.resolve(undefined)) as unknown as typeof infinite.fetchNextPage)
    : infinite.fetchNextPage;

  if (fetchAll) {
    return {
      ...allQuery,
      data: allQuery.data ?? [],
      pages: undefined,
      fetchNextPage,
      hasNextPage: false,
      isFetchingNextPage: false,
    };
  }

  return {
    ...infinite,
    data: pagedData,
    pages: infinite.data?.pages,
    fetchNextPage,
    hasNextPage: infinite.hasNextPage,
    isFetchingNextPage: infinite.isFetchingNextPage,
  };
}
