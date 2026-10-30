/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { type TodoListRequest, todosApi } from '@/lib/api/todos';
import type { ListQuery } from '@/lib/list/types';
import { useRealtimePollingFallback } from '@/lib/realtime/useRealtimePollingFallback';
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
    staleTime: 30 * 1000,
  });
}

export function useTodos({ listQuery, enabled = true, fetchAll = false }: UseTodosOptions) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return usePagedList<TodoDto, TodoListRequest>({
    queryKey: [...todoKeys.all, 'list'],
    listQuery: listQuery as TodoListRequest,
    queryFn: (query) => todosApi.list(query),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 30 * 1000,
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
    staleTime: 15 * 1000,
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
