/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { type TodoListRequest, todosApi } from '@/lib/api/todos';
import type { ListQuery } from '@/lib/list/types';
import { useRealtimePollingFallback } from '@/lib/realtime/useRealtimePollingFallback';
import { STALE_TIME } from '@/lib/store/staleTime';
import { usePagedList } from '@/lib/store/usePagedList';
import type { TodoDto } from '@/types/todo';

export const todoKeys = {
  all: ['todos'] as const,
  list: (listQuery: ListQuery) => [...todoKeys.all, 'list', listQuery] as const,
  detail: (id: number) => [...todoKeys.all, 'detail', id] as const,
  openCount: () => [...todoKeys.all, 'open-count'] as const,
};

type UseTodosOptions = {
  listQuery: ListQuery;
  enabled?: boolean;
  fetchAll?: boolean;
};

export function useTodo(todoId: number, options?: { enabled?: boolean }) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: todoKeys.detail(todoId),
    queryFn: () => todosApi.get(todoId),
    enabled: hasEmberOneUserId && (options?.enabled ?? true),
    staleTime: STALE_TIME.LIVE,
  });
}

export function useTodos({ listQuery, enabled = true, fetchAll = false }: UseTodosOptions) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return usePagedList<TodoDto, TodoListRequest>({
    queryKey: [...todoKeys.all, 'list'],
    listQuery: listQuery as TodoListRequest,
    queryFn: (query) => todosApi.list(query),
    enabled: hasEmberOneUserId && enabled,
    staleTime: STALE_TIME.LIVE,
    fetchAll,
  });
}

export function useOpenTodoCount(options?: { enabled?: boolean }) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  const refetchInterval = useRealtimePollingFallback(60 * 1000);
  return useQuery({
    queryKey: todoKeys.openCount(),
    queryFn: async () => {
      const result = await todosApi.getOpenCount();
      return result.count;
    },
    enabled: hasEmberOneUserId && (options?.enabled ?? true),
    staleTime: STALE_TIME.REALTIME,
    refetchInterval,
  });
}

export function useCompleteTodo() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => todosApi.complete(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: todoKeys.all });
    },
  });
}
