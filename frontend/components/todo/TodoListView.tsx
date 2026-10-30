/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { ListContainer } from '@/components/ui/list';
import { ErrorState } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useTranslation } from '@/hooks/useTranslation';
import { useUrlSyncedListState } from '@/lib/list';
import { TodoListStatusParam, todoListDefinition } from '@/lib/list/todoList';
import { useOpenTodoCount, useTodos } from '@/lib/store';
import { TodoListItem } from './TodoListItem';

function selectedStatuses(raw: unknown): readonly string[] {
  return Array.isArray(raw)
    ? raw.filter((value): value is string => typeof value === 'string')
    : [];
}

export function TodoListView() {
  const t = useTranslation();
  const listState = useUrlSyncedListState(todoListDefinition);
  const statuses = selectedStatuses(listState.filters.status);
  const isDoneOnly = statuses.length === 1 && statuses[0] === TodoListStatusParam.DONE;
  const isAllStatuses = statuses.length === 0;

  const {
    data: items,
    pages,
    isPending,
    isError,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useTodos({ listQuery: listState.listQuery });

  const { data: openCount } = useOpenTodoCount();

  const description = useMemo(() => {
    if (isAllStatuses) {
      const count = pages?.[0]?.totalElements ?? items.length;
      return count === 0 ? t('myArea.todos.allSubtitle') : t('myArea.todos.allCount', { count });
    }
    if (isDoneOnly) {
      const count = pages?.[0]?.totalElements ?? items.length;
      return count === 0 ? t('myArea.todos.doneSubtitle') : t('myArea.todos.doneCount', { count });
    }
    const count = openCount ?? items.length;
    return count === 0 ? t('myArea.todos.subtitle') : t('myArea.todos.count', { count });
  }, [isAllStatuses, isDoneOnly, items.length, openCount, pages, t]);

  const emptyMessage = isDoneOnly
    ? t('todos.emptyDone')
    : isAllStatuses
      ? t('todos.emptyAll')
      : t('todos.empty');

  return (
    <>
      <PageHeader title={t('myArea.todos.title')} description={description} size="compact" />

      <PaginatedListView
        definition={todoListDefinition}
        state={listState}
        totalItems={items.length}
        isPending={isPending}
        error={
          isError ? (
            <ErrorState message={t('todos.loadError')} className="mt-1 sm:mt-0" />
          ) : undefined
        }
        emptyState={paginatedListEmptyCard(emptyMessage)}
        hasNextPage={!!hasNextPage}
        isFetchingNextPage={isFetchingNextPage}
        onLoadMore={() => void fetchNextPage()}
      >
        <ListContainer label={t('myArea.todos.title')} variant="card" rowSeparator="divide">
          {items.map((todo) => (
            <TodoListItem key={todo.id} todo={todo} />
          ))}
        </ListContainer>
      </PaginatedListView>
    </>
  );
}
